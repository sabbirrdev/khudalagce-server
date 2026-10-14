package com.company.efood.sys.services.serviceimpl;

import com.company.efood.sys.entity.Branch;
import com.company.efood.seller.repository.BranchRepo;
import com.company.efood.sys.dto.DeliveryFeeResult;
import com.company.efood.sys.entity.Address;
import com.company.efood.sys.entity.Shop;
import com.company.efood.sys.repository.AddressRepo;
import com.company.efood.sys.services.DeliveryFeeCalculationService;
import com.company.efood.zone.entity.Zone;
import com.company.efood.zone.repository.ZoneRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Enterprise delivery fee calculation — hub + distance + weight model.
 *
 * <p>Formula:
 * <pre>
 *   fee = baseFee
 *       + max(0, distanceKm - freeDistanceKm) * perKmCharge
 *       + max(0, weightKg - freeWeightKg)     * perKgCharge
 *
 *   fee = max(fee, minDeliveryFee)
 * </pre>
 *
 * <p>Origin for distance calculation (priority order):
 * <ol>
 *   <li>Branch address GPS (lat/lon)</li>
 *   <li>Shop GPS (shopLat / shopLon)</li>
 *   <li>Zone hub GPS (hubLat / hubLon)  — zone-level dispatch centre</li>
 *   <li>No GPS available → apply base fee only</li>
 * </ol>
 *
 * <p>Zone resolution (priority order for delivery address):
 * <ol>
 *   <li>address.zone (directly linked by admin)</li>
 *   <li>policeStation / upazila name match in active zones</li>
 *   <li>district name match in active zones</li>
 *   <li>Any active zone configured by admin (platform fallback)</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeliveryFeeCalculationServiceImpl implements DeliveryFeeCalculationService {

    private final BranchRepo branchRepo;
    private final AddressRepo addressRepo;
    private final ZoneRepository zoneRepository;

    @Override
    @Transactional(readOnly = true)
    public DeliveryFeeResult calculate(Long branchId, Long deliveryAddressId, int totalWeightGrams) {
        if (branchId == null) {
            throw new IllegalArgumentException("Branch ID is required for delivery fee calculation");
        }
        if (deliveryAddressId == null) {
            throw new IllegalArgumentException("Delivery address ID is required for delivery fee calculation");
        }

        // 1. Load Branch + delivery Address
        Branch branch = branchRepo.findById(branchId)
                .orElseThrow(() -> new IllegalArgumentException("Branch not found: " + branchId));

        Address deliveryAddress = addressRepo.findById(deliveryAddressId)
                .orElseThrow(() -> new IllegalArgumentException("Delivery address not found: " + deliveryAddressId));

        // 2. Resolve Zone for delivery address (no zone required for seller)
        String deliveryDistrict = nullSafe(deliveryAddress.getDistrict());
        String deliveryPoliceStation = nullSafe(deliveryAddress.getPoliceStation());
        Zone zone = resolveZone(deliveryAddress, deliveryDistrict, deliveryPoliceStation);

        if (zone == null) {
            log.warn("❌ [DELIVERY FEE] No active Zone found for branchId={} addressId={}", branchId, deliveryAddressId);
            return DeliveryFeeResult.builder()
                    .fee(BigDecimal.ZERO)
                    .distanceKm(null)
                    .basis("NONE")
                    .deliverable(false)
                    .reason("Delivery pricing is not configured for your area")
                    .zoneId(null)
                    .zoneName(null)
                    .build();
        }

        // 3. Check deliverability
        if (Boolean.FALSE.equals(zone.getIsDeliverable())) {
            log.warn("❌ [DELIVERY FEE] Zone '{}' (ID={}) is marked non-deliverable", zone.getName(), zone.getId());
            return DeliveryFeeResult.builder()
                    .fee(BigDecimal.ZERO)
                    .distanceKm(null)
                    .basis("ZONE")
                    .deliverable(false)
                    .reason("Delivery is currently not available to " + zone.getName() + " zone")
                    .zoneId(zone.getId())
                    .zoneName(zone.getName())
                    .build();
        }

        // 4. Resolve origin GPS — branch > shop > zone hub
        double[] origin = resolveOriginGps(branch, zone);
        double[] destination = {deliveryAddress.getLat(), deliveryAddress.getLon()};

        boolean originHasGps = origin != null && (origin[0] != 0.0 || origin[1] != 0.0);
        boolean destHasGps = destination[0] != 0.0 || destination[1] != 0.0;

        // 5. Read zone rates (safe defaults)
        BigDecimal base = nvl(zone.getBaseDeliveryFee(), BigDecimal.ZERO);
        BigDecimal perKm = nvl(zone.getPerKmCharge(), BigDecimal.ZERO);
        BigDecimal perKgCharge = nvl(zone.getPerKgCharge(), BigDecimal.ZERO);
        double freeDistKm = zone.getFreeDistanceKm() != null ? zone.getFreeDistanceKm() : 1.0;
        int freeWeightGrams = zone.getFreeWeightGrams() != null ? zone.getFreeWeightGrams() : 1000;
        BigDecimal minFee = zone.getMinDeliveryFee() != null ? zone.getMinDeliveryFee() : base;

        BigDecimal fee = base;
        Double distanceKmResult = null;
        String basis;
        BigDecimal distanceSurcharge = BigDecimal.ZERO;
        BigDecimal weightSurcharge = BigDecimal.ZERO;

        // 6. Distance surcharge
        if (originHasGps && destHasGps) {
            double rawDist = haversine(origin[0], origin[1], destination[0], destination[1]);
            double distanceKm = Math.round(rawDist * 100.0) / 100.0;
            distanceKmResult = distanceKm;

            if (distanceKm > freeDistKm && perKm.compareTo(BigDecimal.ZERO) > 0) {
                distanceSurcharge = perKm.multiply(BigDecimal.valueOf(distanceKm - freeDistKm));
                fee = fee.add(distanceSurcharge);
            }
            basis = "HUB_DISTANCE";
            log.info("🚚 [DELIVERY FEE] origin=({},{}) → dest=({},{}) dist={}km freeThreshold={}km perKm={}",
                    origin[0], origin[1], destination[0], destination[1], distanceKm, freeDistKm, perKm);
        } else {
            basis = "BASE_NO_COORDS";
            log.warn("⚠️ [DELIVERY FEE] Missing GPS for branch #{} or address #{} — base fee only", branchId, deliveryAddressId);
        }

        // 7. Weight surcharge
        if (totalWeightGrams > freeWeightGrams && perKgCharge.compareTo(BigDecimal.ZERO) > 0) {
            double excessKg = (totalWeightGrams - freeWeightGrams) / 1000.0;
            weightSurcharge = perKgCharge.multiply(BigDecimal.valueOf(excessKg));
            fee = fee.add(weightSurcharge);
            log.info("⚖️ [DELIVERY FEE] Weight {}g, free {}g, excess {}kg @ {}/kg → +{}",
                    totalWeightGrams, freeWeightGrams, excessKg, perKgCharge, weightSurcharge);
            basis = basis + "_WEIGHT";
        }

        // 8. Apply minimum fee floor
        if (fee.compareTo(minFee) < 0) {
            fee = minFee;
        }
        fee = fee.setScale(2, RoundingMode.HALF_UP);
        distanceSurcharge = distanceSurcharge.setScale(2, RoundingMode.HALF_UP);
        weightSurcharge = weightSurcharge.setScale(2, RoundingMode.HALF_UP);

        log.info("✅ [DELIVERY FEE] Zone='{}' base={} distSurcharge={} weightSurcharge={} dist={}km weight={}g → finalFee={}",
                zone.getName(), base, distanceSurcharge, weightSurcharge, distanceKmResult, totalWeightGrams, fee);

        return DeliveryFeeResult.builder()
                .fee(fee)
                .baseFee(base.setScale(2, RoundingMode.HALF_UP))
                .distanceSurcharge(distanceSurcharge)
                .weightSurcharge(weightSurcharge)
                .distanceKm(distanceKmResult)
                .basis(basis)
                .deliverable(true)
                .reason(null)
                .zoneId(zone.getId())
                .zoneName(zone.getName())
                .totalWeightGrams(totalWeightGrams)
                .build();
    }

    /**
     * Resolve origin GPS in priority: branch address → shop GPS → zone hub.
     */
    private double[] resolveOriginGps(Branch branch, Zone zone) {
        // Priority 1: branch address GPS
        if (branch.getAddress() != null) {
            double lat = branch.getAddress().getLat();
            double lon = branch.getAddress().getLon();
            if (lat != 0.0 || lon != 0.0) {
                log.debug("📍 [DELIVERY FEE] Using branch address GPS ({},{})", lat, lon);
                return new double[]{lat, lon};
            }
        }
        // Priority 2: shop GPS coordinates
        Shop shop = branch.getShop();
        if (shop != null && shop.getShopLat() != null && shop.getShopLon() != null
                && (shop.getShopLat() != 0.0 || shop.getShopLon() != 0.0)) {
            log.debug("📍 [DELIVERY FEE] Using shop GPS ({},{})", shop.getShopLat(), shop.getShopLon());
            return new double[]{shop.getShopLat(), shop.getShopLon()};
        }
        // Priority 3: zone hub GPS
        if (zone != null && zone.getHubLat() != null && zone.getHubLon() != null
                && (zone.getHubLat() != 0.0 || zone.getHubLon() != 0.0)) {
            log.debug("📍 [DELIVERY FEE] Using zone hub GPS ({},{})", zone.getHubLat(), zone.getHubLon());
            return new double[]{zone.getHubLat(), zone.getHubLon()};
        }
        return null;
    }

    /**
     * Resolve the Zone for the delivery address.
     * Priority: address.zone → upazila/policeStation → district → any active zone.
     */
    private Zone resolveZone(Address address, String district, String policeStation) {
        if (address != null && address.getZone() != null && address.getZone().getId() != null) {
            Zone z = zoneRepository.findById(address.getZone().getId()).orElse(null);
            if (z != null && Boolean.TRUE.equals(z.getActive())) return z;
        }
        if (!policeStation.isEmpty()) {
            List<Zone> zones = zoneRepository.findActiveByUpazilaName(policeStation);
            if (!zones.isEmpty()) return zones.get(0);
        }
        if (!district.isEmpty()) {
            List<Zone> zones = zoneRepository.findActiveByDistrictName(district);
            if (!zones.isEmpty()) return zones.get(0);
        }
        List<Zone> active = zoneRepository.findByActiveTrue();
        return active.isEmpty() ? null : active.get(0);
    }

    private String nullSafe(String s) {
        if (s == null || s.trim().isEmpty() || s.equalsIgnoreCase("N/A")) return "";
        return s.trim();
    }

    private BigDecimal nvl(BigDecimal v, BigDecimal def) {
        return v != null ? v : def;
    }

    /**
     * Haversine formula — straight-line geodesic distance in km.
     */
    private double haversine(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return R * 2.0 * Math.atan2(Math.sqrt(a), Math.sqrt(1.0 - a));
    }
}

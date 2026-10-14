package com.company.efood.zone.service;

import com.company.efood.zone.dto.DistrictDto;
import com.company.efood.zone.dto.DivisionDto;
import com.company.efood.zone.dto.UpazilaDto;
import com.company.efood.zone.dto.ZoneDto;
import com.company.efood.zone.entity.District;
import com.company.efood.zone.entity.Division;
import com.company.efood.zone.entity.Upazila;
import com.company.efood.zone.entity.Zone;
import com.company.efood.zone.model.DeliveryFeeCalcResponse;
import com.company.efood.zone.repository.DistrictRepository;
import com.company.efood.zone.repository.DivisionRepository;
import com.company.efood.zone.repository.UpazilaRepository;
import com.company.efood.zone.repository.ZoneRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ZoneService {
    private final DivisionRepository divisionRepo;
    private final DistrictRepository districtRepo;
    private final UpazilaRepository upazilaRepo;
    private final ZoneRepository zoneRepo;
    private final ModelMapper modelMapper;

    // ────────────────────────────────────────────────────────────────────────
    // Division CRUD
    // ────────────────────────────────────────────────────────────────────────
    public DivisionDto saveDivision(DivisionDto dto, Long userId) {
        Division division;
        if (dto.getId() != null) {
            division = divisionRepo.findById(dto.getId()).orElse(new Division());
            division.setUpdateUser(userId);
            division.setUpdateDate(java.time.LocalDateTime.now());
        } else {
            division = new Division();
            division.setEntryUser(userId);
            division.setEntryDate(java.time.LocalDateTime.now());
        }

        division.setName(dto.getName());
        if (dto.getNameBn() != null) division.setNameBn(dto.getNameBn());
        Boolean status = dto.getActive() != null ? dto.getActive() : dto.getIsActive();
        division.setActive(status != null ? status : true);

        division = divisionRepo.save(division);
        return mapDivisionToDto(division);
    }

    public List<DivisionDto> getAllDivisions() {
        return divisionRepo.findByActiveTrue().stream()
                .map(this::mapDivisionToDto)
                .collect(Collectors.toList());
    }

    public List<DivisionDto> getAllDivisionsAdmin() {
        return divisionRepo.findAll().stream()
                .map(this::mapDivisionToDto)
                .collect(Collectors.toList());
    }

    private DivisionDto mapDivisionToDto(Division division) {
        DivisionDto dto = new DivisionDto();
        dto.setId(division.getId());
        dto.setName(division.getName());
        dto.setNameBn(division.getNameBn());
        dto.setActive(division.getActive());
        dto.setIsActive(division.getActive());
        return dto;
    }

    // ────────────────────────────────────────────────────────────────────────
    // District CRUD
    // ────────────────────────────────────────────────────────────────────────
    public DistrictDto saveDistrict(DistrictDto dto, Long userId) {
        District district;
        if (dto.getId() != null) {
            district = districtRepo.findById(dto.getId()).orElse(new District());
            district.setUpdateUser(userId);
            district.setUpdateDate(java.time.LocalDateTime.now());
        } else {
            district = new District();
            district.setEntryUser(userId);
            district.setEntryDate(java.time.LocalDateTime.now());
        }

        district.setName(dto.getName());
        if (dto.getNameBn() != null) district.setNameBn(dto.getNameBn());
        Boolean status = dto.getActive() != null ? dto.getActive() : dto.getIsActive();
        district.setActive(status != null ? status : true);

        if (dto.getDivisionId() != null) {
            Division division = divisionRepo.findById(dto.getDivisionId()).orElse(null);
            district.setDivision(division);
        }

        district = districtRepo.save(district);
        return mapDistrictToDto(district);
    }

    public List<DistrictDto> getDistrictsByDivision(Long divisionId) {
        return districtRepo.findByDivisionId(divisionId).stream()
                .map(this::mapDistrictToDto)
                .collect(Collectors.toList());
    }

    public List<DistrictDto> getAllDistricts() {
        return districtRepo.findAll().stream()
                .map(this::mapDistrictToDto)
                .collect(Collectors.toList());
    }

    private DistrictDto mapDistrictToDto(District district) {
        DistrictDto dto = new DistrictDto();
        dto.setId(district.getId());
        dto.setName(district.getName());
        dto.setNameBn(district.getNameBn());
        dto.setActive(district.getActive());
        dto.setIsActive(district.getActive());
        if (district.getDivision() != null) {
            dto.setDivisionId(district.getDivision().getId());
            dto.setDivisionName(district.getDivision().getName());
        }
        return dto;
    }

    // ────────────────────────────────────────────────────────────────────────
    // Upazila CRUD
    // ────────────────────────────────────────────────────────────────────────
    public UpazilaDto saveUpazila(UpazilaDto dto, Long userId) {
        Upazila upazila;
        if (dto.getId() != null) {
            upazila = upazilaRepo.findById(dto.getId()).orElse(new Upazila());
            upazila.setUpdateUser(userId);
            upazila.setUpdateDate(java.time.LocalDateTime.now());
        } else {
            upazila = new Upazila();
            upazila.setEntryUser(userId);
            upazila.setEntryDate(java.time.LocalDateTime.now());
        }

        upazila.setName(dto.getName());
        if (dto.getNameBn() != null) upazila.setNameBn(dto.getNameBn());
        Boolean status = dto.getActive() != null ? dto.getActive() : dto.getIsActive();
        upazila.setActive(status != null ? status : true);

        if (dto.getDistrictId() != null) {
            District district = districtRepo.findById(dto.getDistrictId()).orElse(null);
            upazila.setDistrict(district);
        }

        upazila = upazilaRepo.save(upazila);
        return mapUpazilaToDto(upazila);
    }

    public List<UpazilaDto> getUpazilasByDistrict(Long districtId) {
        return upazilaRepo.findByDistrictId(districtId).stream()
                .map(this::mapUpazilaToDto)
                .collect(Collectors.toList());
    }

    public List<UpazilaDto> getAllUpazilas() {
        return upazilaRepo.findAll().stream()
                .map(this::mapUpazilaToDto)
                .collect(Collectors.toList());
    }

    private UpazilaDto mapUpazilaToDto(Upazila upazila) {
        UpazilaDto dto = new UpazilaDto();
        dto.setId(upazila.getId());
        dto.setName(upazila.getName());
        dto.setNameBn(upazila.getNameBn());
        dto.setActive(upazila.getActive());
        dto.setIsActive(upazila.getActive());
        if (upazila.getDistrict() != null) {
            dto.setDistrictId(upazila.getDistrict().getId());
            dto.setDistrictName(upazila.getDistrict().getName());
        }
        return dto;
    }

    // ────────────────────────────────────────────────────────────────────────
    // Zone CRUD
    // ────────────────────────────────────────────────────────────────────────
    public ZoneDto saveZone(ZoneDto dto, Long userId) {
        Zone zone;
        if (dto.getId() != null) {
            zone = zoneRepo.findById(dto.getId()).orElse(new Zone());
            zone.setUpdateUser(userId);
            zone.setUpdateDate(java.time.LocalDateTime.now());
        } else {
            zone = new Zone();
            zone.setEntryUser(userId);
            zone.setEntryDate(java.time.LocalDateTime.now());
        }

        zone.setName(dto.getName());
        if (dto.getNameBn() != null) zone.setNameBn(dto.getNameBn());
        if (dto.getBaseDeliveryFee() != null) zone.setBaseDeliveryFee(dto.getBaseDeliveryFee());
        if (dto.getPerKmCharge() != null) zone.setPerKmCharge(dto.getPerKmCharge());
        if (dto.getHubLat() != null) zone.setHubLat(dto.getHubLat());
        if (dto.getHubLon() != null) zone.setHubLon(dto.getHubLon());
        if (dto.getGeoFencePolygon() != null) zone.setGeoFencePolygon(dto.getGeoFencePolygon());

        Boolean status = dto.getActive() != null ? dto.getActive() : dto.getIsActive();
        zone.setActive(status != null ? status : true);

        if (dto.getIsDeliverable() != null) {
            zone.setIsDeliverable(dto.getIsDeliverable());
        }

        if (dto.getUpazilaId() != null) {
            Upazila upazila = upazilaRepo.findById(dto.getUpazilaId()).orElse(null);
            zone.setUpazila(upazila);
        }

        zone = zoneRepo.save(zone);
        return mapZoneToDto(zone);
    }

    public ZoneDto updateZone(Long id, ZoneDto dto, Long userId) {
        dto.setId(id);
        return saveZone(dto, userId);
    }

    public boolean deleteZone(Long id) {
        if (zoneRepo.existsById(id)) {
            zoneRepo.deleteById(id);
            return true;
        }
        return false;
    }

    public boolean deleteAllZones() {
        zoneRepo.deleteAll();
        return true;
    }

    public boolean deleteUpazila(Long id) {
        if (upazilaRepo.existsById(id)) {
            upazilaRepo.deleteById(id);
            return true;
        }
        return false;
    }

    public boolean deleteDistrict(Long id) {
        if (districtRepo.existsById(id)) {
            districtRepo.deleteById(id);
            return true;
        }
        return false;
    }

    public boolean deleteDivision(Long id) {
        if (divisionRepo.existsById(id)) {
            divisionRepo.deleteById(id);
            return true;
        }
        return false;
    }

    public List<ZoneDto> getZonesByUpazila(Long upazilaId) {
        List<Zone> zones = zoneRepo.findByUpazilaIdAndActiveTrue(upazilaId);
        // Fallback: If no zone exists directly on this upazila,
        // lookup active deliverable zones in the same district so customer is never stuck
        if (zones.isEmpty() && upazilaId != null) {
            Upazila upazila = upazilaRepo.findById(upazilaId).orElse(null);
            if (upazila != null && upazila.getDistrict() != null) {
                zones = zoneRepo.findByUpazilaDistrictId(upazila.getDistrict().getId()).stream()
                        .filter(z -> Boolean.TRUE.equals(z.getActive()) && Boolean.TRUE.equals(z.getIsDeliverable()))
                        .collect(Collectors.toList());
            }
        }
        return zones.stream()
                .map(this::mapZoneToDto)
                .collect(Collectors.toList());
    }

    public List<ZoneDto> getActiveZones() {
        return zoneRepo.findByActiveTrue().stream()
                .map(this::mapZoneToDto)
                .collect(Collectors.toList());
    }

    public List<ZoneDto> getAllZonesAdmin() {
        return zoneRepo.findAll().stream()
                .map(this::mapZoneToDto)
                .collect(Collectors.toList());
    }

    public ZoneDto getZoneById(Long id) {
        return zoneRepo.findById(id).map(this::mapZoneToDto).orElse(null);
    }

    private ZoneDto mapZoneToDto(Zone zone) {
        ZoneDto dto = new ZoneDto();
        dto.setId(zone.getId());
        dto.setName(zone.getName());
        dto.setNameBn(zone.getNameBn());
        dto.setBaseDeliveryFee(zone.getBaseDeliveryFee());
        dto.setPerKmCharge(zone.getPerKmCharge());
        dto.setHubLat(zone.getHubLat());
        dto.setHubLon(zone.getHubLon());
        dto.setGeoFencePolygon(zone.getGeoFencePolygon());
        dto.setActive(zone.getActive());
        dto.setIsActive(zone.getActive());
        dto.setIsDeliverable(zone.getIsDeliverable());
        if (zone.getUpazila() != null) {
            dto.setUpazilaId(zone.getUpazila().getId());
            dto.setUpazilaName(zone.getUpazila().getName());
            if (zone.getUpazila().getDistrict() != null) {
                dto.setDistrictName(zone.getUpazila().getDistrict().getName());
                if (zone.getUpazila().getDistrict().getDivision() != null) {
                    dto.setDivisionName(zone.getUpazila().getDistrict().getDivision().getName());
                }
            }
        }
        return dto;
    }

    public DeliveryFeeCalcResponse calculateDeliveryFee(Long zoneId, double customerLat, double customerLon) {
        Zone zone = zoneRepo.findById(zoneId).orElseThrow(() -> new RuntimeException("Zone not found"));
        double distKm = 0;
        if (zone.getHubLat() != null && zone.getHubLon() != null) {
            distKm = haversineDistanceKm(zone.getHubLat(), zone.getHubLon(), customerLat, customerLon);
        }

        BigDecimal extraCharge = BigDecimal.ZERO;
        if (distKm > 1.0) {
            extraCharge = zone.getPerKmCharge().multiply(BigDecimal.valueOf(distKm - 1.0));
        }
        BigDecimal totalCharge = zone.getBaseDeliveryFee().add(extraCharge);

        DeliveryFeeCalcResponse response = new DeliveryFeeCalcResponse();
        response.setZoneId(zoneId);
        response.setZoneName(zone.getName());
        response.setDistanceKm(distKm);
        response.setBaseDeliveryFee(zone.getBaseDeliveryFee());
        response.setExtraCharge(extraCharge);
        response.setTotalCharge(totalCharge);
        return response;
    }

    private double haversineDistanceKm(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}

package com.company.efood.sys.services.serviceimpl;

import com.company.efood.seller.repository.BranchRepo;
import com.company.efood.sys.dto.DeliveryFeeResult;
import com.company.efood.sys.entity.Address;
import com.company.efood.sys.entity.Branch;
import com.company.efood.sys.repository.AddressRepo;
import com.company.efood.zone.entity.District;
import com.company.efood.zone.entity.Upazila;
import com.company.efood.zone.entity.Zone;
import com.company.efood.zone.repository.ZoneRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeliveryFeeCalculationServiceImplTest {

    @Mock private BranchRepo branchRepo;
    @Mock private AddressRepo addressRepo;
    @Mock private ZoneRepository zoneRepository;

    @InjectMocks
    private DeliveryFeeCalculationServiceImpl service;

    private Branch branch;
    private Address branchAddress;
    private Address customerAddress;

    @BeforeEach
    void setUp() {
        branchAddress = new Address();
        branchAddress.setId(10L);
        branchAddress.setDistrict("Dhaka");
        branchAddress.setPoliceStation("Dhanmondi");
        branchAddress.setLat(23.7465);
        branchAddress.setLon(90.3760);

        branch = new Branch();
        branch.setId(1L);
        branch.setName("Dhanmondi Branch");
        branch.setAddress(branchAddress);

        customerAddress = new Address();
        customerAddress.setId(20L);
        customerAddress.setDistrict("Dhaka");
        customerAddress.setPoliceStation("Dhanmondi");
        customerAddress.setLat(23.7500);
        customerAddress.setLon(90.3800);
    }

    // ── Helper: build a Zone with district + upazila ────────────────────────
    private Zone buildZone(String districtName, String upazilaName,
                           double base, double perKm, boolean deliverable) {
        District d = new District();
        d.setName(districtName);

        Upazila u = new Upazila();
        u.setName(upazilaName);
        u.setDistrict(d);

        Zone z = new Zone();
        z.setId(100L);
        z.setName(upazilaName + " Zone");
        z.setUpazila(u);
        z.setBaseDeliveryFee(BigDecimal.valueOf(base));
        z.setPerKmCharge(BigDecimal.valueOf(perKm));
        z.setIsDeliverable(deliverable);
        z.setActive(true);
        z.setHubLat(23.7465);
        z.setHubLon(90.3760);
        return z;
    }

    // ════════════════════════════════════════════════════════════════════════
    // Zone-based fee tests
    // ════════════════════════════════════════════════════════════════════════

    @Test
    void shouldUseZoneFeeWhenZoneMatchesUpazila() {
        Zone zone = buildZone("Dhaka", "Dhanmondi", 30.0, 5.0, true);

        when(branchRepo.findById(1L)).thenReturn(Optional.of(branch));
        when(addressRepo.findById(20L)).thenReturn(Optional.of(customerAddress));
        when(zoneRepository.findActiveByUpazilaName("Dhanmondi")).thenReturn(List.of(zone));

        DeliveryFeeResult result = service.calculate(1L, 20L);

        assertTrue(result.isDeliverable());
        // Short distance (<1 km) → base fee only, no extra per-km charge
        assertEquals(0, BigDecimal.valueOf(30.0).compareTo(result.getFee()));
        assertNull(result.getReason());
        assertEquals(100L, result.getZoneId());
    }

    @Test
    void shouldUseZoneFeeWithPerKmChargeWhenDistanceOver1km() {
        // Customer is in Mirpur, branch in Dhanmondi (~7 km apart)
        customerAddress.setDistrict("Dhaka");
        customerAddress.setPoliceStation("Mirpur");
        customerAddress.setLat(23.8103);
        customerAddress.setLon(90.3654);

        Zone zone = buildZone("Dhaka", "Mirpur", 40.0, 8.0, true);

        when(branchRepo.findById(1L)).thenReturn(Optional.of(branch));
        when(addressRepo.findById(20L)).thenReturn(Optional.of(customerAddress));
        when(zoneRepository.findActiveByUpazilaName("Mirpur")).thenReturn(List.of(zone));

        DeliveryFeeResult result = service.calculate(1L, 20L);

        assertTrue(result.isDeliverable());
        assertEquals("ZONE_DISTANCE", result.getBasis());
        assertNotNull(result.getDistanceKm());
        assertTrue(result.getDistanceKm() > 5.0); // ~7km
        assertTrue(result.getFee().compareTo(BigDecimal.valueOf(40.0)) > 0); // fee > base
        assertNull(result.getReason());
    }

    @Test
    void shouldReturnNotDeliverableWhenZoneIsNotDeliverable() {
        Zone zone = buildZone("Dhaka", "Dhanmondi", 30.0, 5.0, false);

        when(branchRepo.findById(1L)).thenReturn(Optional.of(branch));
        when(addressRepo.findById(20L)).thenReturn(Optional.of(customerAddress));
        when(zoneRepository.findActiveByUpazilaName("Dhanmondi")).thenReturn(List.of(zone));

        DeliveryFeeResult result = service.calculate(1L, 20L);

        assertFalse(result.isDeliverable());
        assertEquals("ZONE", result.getBasis());
        assertNotNull(result.getReason());
        assertTrue(result.getReason().contains("not available"));
    }

    @Test
    void shouldFallbackToDistrictZoneWhenUpazilaNotMatched() {
        // policeStation won't match any upazila; district will match
        customerAddress.setPoliceStation("UnknownArea");
        customerAddress.setDistrict("Dhaka");

        Zone zone = buildZone("Dhaka", "Dhanmondi", 50.0, 6.0, true);

        when(branchRepo.findById(1L)).thenReturn(Optional.of(branch));
        when(addressRepo.findById(20L)).thenReturn(Optional.of(customerAddress));
        when(zoneRepository.findActiveByUpazilaName("UnknownArea")).thenReturn(Collections.emptyList());
        when(zoneRepository.findActiveByDistrictName("Dhaka")).thenReturn(List.of(zone));

        DeliveryFeeResult result = service.calculate(1L, 20L);

        assertTrue(result.isDeliverable());
        // base fee (30 taka) should be applied; distance is short so no extra
        assertTrue(result.getFee().compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    void shouldFallbackToAnyActiveZoneWhenNoDistrictMatch() {
        customerAddress.setPoliceStation("RemoteVillage");
        customerAddress.setDistrict("UnknownDistrict");

        Zone fallbackZone = buildZone("Dhaka", "Dhanmondi", 60.0, 7.0, true);

        when(branchRepo.findById(1L)).thenReturn(Optional.of(branch));
        when(addressRepo.findById(20L)).thenReturn(Optional.of(customerAddress));
        when(zoneRepository.findActiveByUpazilaName("RemoteVillage")).thenReturn(Collections.emptyList());
        when(zoneRepository.findActiveByDistrictName("UnknownDistrict")).thenReturn(Collections.emptyList());
        when(zoneRepository.findByActiveTrue()).thenReturn(List.of(fallbackZone));

        DeliveryFeeResult result = service.calculate(1L, 20L);

        assertTrue(result.isDeliverable());
        assertTrue(result.getFee().compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    void shouldReturnNotDeliverableWhenNoZonesConfigured() {
        customerAddress.setPoliceStation("NoZoneArea");
        customerAddress.setDistrict("NoZoneDistrict");

        when(branchRepo.findById(1L)).thenReturn(Optional.of(branch));
        when(addressRepo.findById(20L)).thenReturn(Optional.of(customerAddress));
        when(zoneRepository.findActiveByUpazilaName(anyString())).thenReturn(Collections.emptyList());
        when(zoneRepository.findActiveByDistrictName(anyString())).thenReturn(Collections.emptyList());
        when(zoneRepository.findByActiveTrue()).thenReturn(Collections.emptyList());

        DeliveryFeeResult result = service.calculate(1L, 20L);

        assertFalse(result.isDeliverable());
        assertEquals("NONE", result.getBasis());
        assertTrue(result.getReason().contains("Delivery pricing is not configured"));
    }

    @Test
    void shouldApplyBaseFeeOnlyWhenCoordinatesAreMissing() {
        // lat/lon = 0.0 means no GPS data
        customerAddress.setLat(0.0);
        customerAddress.setLon(0.0);
        customerAddress.setPoliceStation("Sonadanga");
        customerAddress.setDistrict("Khulna");

        Zone zone = buildZone("Khulna", "Sonadanga", 45.0, 5.0, true);
        zone.setHubLat(0.0);
        zone.setHubLon(0.0);

        when(branchRepo.findById(1L)).thenReturn(Optional.of(branch));
        when(addressRepo.findById(20L)).thenReturn(Optional.of(customerAddress));
        when(zoneRepository.findActiveByUpazilaName("Sonadanga")).thenReturn(List.of(zone));

        DeliveryFeeResult result = service.calculate(1L, 20L);

        assertTrue(result.isDeliverable());
        assertEquals("ZONE_BASE_NO_COORDS", result.getBasis());
        // Exact base fee (no distance computation possible)
        assertEquals(0, BigDecimal.valueOf(45.0).compareTo(result.getFee()));
        assertNull(result.getDistanceKm());
    }

    @Test
    void shouldThrowExceptionWhenBranchOrAddressMissing() {
        when(branchRepo.findById(999L)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> service.calculate(999L, 20L));

        when(branchRepo.findById(1L)).thenReturn(Optional.of(branch));
        when(addressRepo.findById(888L)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> service.calculate(1L, 888L));
    }

    @Test
    void shouldUseDirectlyLinkedZoneOnAddress() {
        Zone linkedZone = buildZone("Dhaka", "Gulshan", 80.0, 10.0, true);
        linkedZone.setId(200L);

        // Simulate address having a Zone FK set
        customerAddress.setZone(linkedZone);
        customerAddress.setPoliceStation("Gulshan");

        when(branchRepo.findById(1L)).thenReturn(Optional.of(branch));
        when(addressRepo.findById(20L)).thenReturn(Optional.of(customerAddress));
        when(zoneRepository.findById(200L)).thenReturn(Optional.of(linkedZone));

        DeliveryFeeResult result = service.calculate(1L, 20L);

        assertTrue(result.isDeliverable());
        assertEquals(200L, result.getZoneId());
    }
}

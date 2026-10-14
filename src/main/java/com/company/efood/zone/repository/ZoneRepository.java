package com.company.efood.zone.repository;

import com.company.efood.zone.entity.Zone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ZoneRepository extends JpaRepository<Zone, Long> {
    List<Zone> findByUpazilaId(Long upazilaId);
    List<Zone> findByUpazilaIdAndActiveTrue(Long upazilaId);
    List<Zone> findByActiveTrue();
    List<Zone> findByUpazilaDistrictId(Long districtId);
    List<Zone> findByUpazilaDistrictDivisionId(Long divisionId);

    /**
     * Find active + deliverable zones whose district name matches the delivery address's district.
     * Used by DeliveryFeeCalculationServiceImpl as the first (highest-priority) lookup.
     */
    @Query("SELECT z FROM Zone z " +
           "WHERE z.active = true AND z.isDeliverable = true " +
           "AND LOWER(TRIM(z.upazila.district.name)) = LOWER(TRIM(:districtName)) " +
           "ORDER BY z.id ASC")
    List<Zone> findActiveByDistrictName(@Param("districtName") String districtName);

    /**
     * Find active + deliverable zones whose upazila name matches the policeStation field
     * of the delivery address (common convention in BD addresses).
     */
    @Query("SELECT z FROM Zone z " +
           "WHERE z.active = true AND z.isDeliverable = true " +
           "AND LOWER(TRIM(z.upazila.name)) = LOWER(TRIM(:upazilaName)) " +
           "ORDER BY z.id ASC")
    List<Zone> findActiveByUpazilaName(@Param("upazilaName") String upazilaName);
}


package com.company.efood.raider.repository;

import com.company.efood.raider.entity.Raider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RaiderRepo extends JpaRepository<Raider,Long> {
    Optional<Raider> findByAppUserId(Long appUserId);
    List<Raider> findByIsAvailableTrue();
    List<Raider> findByIsAvailableTrueOrderByIdDesc();
    List<Raider> findByIsAvailableTrueAndVehicleTypeIgnoreCaseOrderByIdDesc(String vehicleType);
    Optional<Raider> findFirstByIsAvailableTrueAndVehicleTypeIgnoreCase(String vehicleType);

    /** Return all raiders awaiting admin approval. */
    List<Raider> findByIsApprovedFalseOrIsApprovedIsNull();

    /** Count raiders awaiting admin approval. */
    long countByIsApprovedFalseOrIsApprovedIsNull();
}

package com.company.efood.sys.repository;

import com.company.efood.sys.entity.VehicleType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VehicleTypeRepo extends JpaRepository<VehicleType, Long> {
    Optional<VehicleType> findByCodeIgnoreCase(String code);
    Optional<VehicleType> findByNameIgnoreCase(String name);
    List<VehicleType> findByActiveTrueOrderByIdAsc();
}

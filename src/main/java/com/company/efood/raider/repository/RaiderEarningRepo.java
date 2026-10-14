package com.company.efood.raider.repository;

import com.company.efood.raider.entity.RaiderEarning;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RaiderEarningRepo extends JpaRepository<RaiderEarning, Long> {
    Optional<RaiderEarning> findByOrderId(Long orderId);
    List<RaiderEarning> findByRaiderId(Long raiderId);
}

package com.company.efood.sys.repository;

import com.company.efood.sys.entity.RiderPayoutPolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RiderPayoutPolicyRepo extends JpaRepository<RiderPayoutPolicy, Long> {
    Optional<RiderPayoutPolicy> findFirstByIsDefaultTrueAndActiveTrue();
    List<RiderPayoutPolicy> findByActiveTrueOrderByIdDesc();
}

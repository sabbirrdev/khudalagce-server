package com.company.efood.sys.repository;

import com.company.efood.sys.entity.PayoutDisbursement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PayoutDisbursementRepo extends JpaRepository<PayoutDisbursement, Long> {
    List<PayoutDisbursement> findByBeneficiaryUserIdAndBeneficiaryTypeOrderByDisbursedAtDesc(Long userId, String type);
    Page<PayoutDisbursement> findAllByOrderByDisbursedAtDesc(Pageable pageable);
    Optional<PayoutDisbursement> findByBeneficiaryUserIdAndBeneficiaryTypeAndWeekLabel(Long userId, String type, String weekLabel);
}

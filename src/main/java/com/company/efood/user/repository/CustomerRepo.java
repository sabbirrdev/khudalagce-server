package com.company.efood.user.repository;

import com.company.efood.user.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerRepo extends JpaRepository<Customer, Long> {
    Optional<Customer> findByAppUserId(Long appUserId);
    Optional<Customer> findByReferralCode(String referralCode);
    Optional<Customer> findByReferralCodeIgnoreCase(String referralCode);

    /** Number of referees of {@code referrerId} who have already had their reward credited. */
    @Query("SELECT COUNT(c) FROM Customer c WHERE c.referredBy.id = :referrerId AND c.referralRewardCredited = true")
    long countReferredAndRewarded(@Param("referrerId") Long referrerId);

    /** Total number of friends who joined using {@code referrerId}'s referral code. */
    long countByReferredById(Long referrerId);
}

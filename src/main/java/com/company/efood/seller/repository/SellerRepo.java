package com.company.efood.seller.repository;

import com.company.efood.seller.dto.SellerDto;
import com.company.efood.seller.entity.Seller;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SellerRepo extends JpaRepository<Seller,Long> {
    Optional<Seller> findByAppUserId(Long appUserId);
    Optional<Seller> findByAppUserId(int appUserId);

    /** Return all sellers awaiting admin approval. */
    List<Seller> findByIsApprovedFalseOrIsApprovedIsNull();

    /** Count sellers awaiting admin approval. */
    long countByIsApprovedFalseOrIsApprovedIsNull();
}


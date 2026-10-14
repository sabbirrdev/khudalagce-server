package com.company.efood.sys.services;

import com.company.efood.seller.entity.Seller;
import com.company.efood.sys.entity.CommissionPolicy;

import java.math.BigDecimal;

/**
 * Dynamic Category-Based Seller Commission Calculation Service.
 * Traces from Seller -> ShopCategory -> CommissionPolicy to compute the order commission.
 */
public interface SellerCommissionCalculationService {

    /**
     * Calculate commission fee for an order based on the Seller's ShopCategory Commission Policy.
     * Traces Seller -> ShopCategory -> CommissionPolicy.
     *
     * @param seller The Seller entity
     * @param orderAmount The product selling order amount (BigDecimal)
     * @return Calculated commission fee with 4 decimal places precision (HALF_UP)
     */
    BigDecimal calculateCommissionFee(Seller seller, BigDecimal orderAmount);

    /**
     * Calculate commission fee for a seller by seller ID and order amount.
     */
    BigDecimal calculateCommissionFee(Long sellerId, BigDecimal orderAmount);

    /**
     * Retrieve the active commission percentage / rate for a seller.
     */
    BigDecimal getCommissionRate(Seller seller);

    /**
     * Retrieve the active CommissionPolicy for a seller by tracing Seller -> ShopCategory -> CommissionPolicy.
     */
    CommissionPolicy getCommissionPolicyForSeller(Seller seller);
}

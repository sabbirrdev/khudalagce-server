package com.company.efood.sys.services.serviceimpl;

import com.company.efood.seller.entity.Seller;
import com.company.efood.seller.repository.SellerRepo;
import com.company.efood.sys.entity.CommissionPolicy;
import com.company.efood.sys.entity.ShopCategory;
import com.company.efood.sys.repository.CommissionPolicyRepo;
import com.company.efood.sys.services.SellerCommissionCalculationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SellerCommissionCalculationServiceImpl implements SellerCommissionCalculationService {

    private final SellerRepo sellerRepo;
    private final CommissionPolicyRepo commissionPolicyRepo;

    private static final BigDecimal DEFAULT_COMMISSION_RATE = new BigDecimal("10.0000");

    @Override
    public BigDecimal calculateCommissionFee(Seller seller, BigDecimal orderAmount) {
        if (orderAmount == null || orderAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        }

        BigDecimal rate = getCommissionRate(seller);
        BigDecimal percentageFee = orderAmount.multiply(rate)
                .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);

        // Check if fixed amount also applies from policy
        CommissionPolicy policy = getCommissionPolicyForSeller(seller);
        if (policy != null && policy.getFixedAmount() != null && policy.getFixedAmount() > 0) {
            BigDecimal fixed = BigDecimal.valueOf(policy.getFixedAmount()).setScale(4, RoundingMode.HALF_UP);
            if (policy.getPercentage() == null || policy.getPercentage() <= 0) {
                return fixed;
            }
            return percentageFee.add(fixed).setScale(4, RoundingMode.HALF_UP);
        }

        return percentageFee;
    }

    @Override
    public BigDecimal calculateCommissionFee(Long sellerId, BigDecimal orderAmount) {
        if (sellerId == null) {
            return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        }
        Seller seller = sellerRepo.findById(sellerId).orElse(null);
        return calculateCommissionFee(seller, orderAmount);
    }

    @Override
    public BigDecimal getCommissionRate(Seller seller) {
        CommissionPolicy policy = getCommissionPolicyForSeller(seller);
        if (policy != null && policy.getPercentage() != null && policy.getPercentage() > 0) {
            return BigDecimal.valueOf(policy.getPercentage()).setScale(4, RoundingMode.HALF_UP);
        }

        // Fallback: check global active policies
        try {
            List<CommissionPolicy> activePolicies = commissionPolicyRepo.findAll();
            LocalDate today = LocalDate.now();
            for (CommissionPolicy p : activePolicies) {
                if (Boolean.TRUE.equals(p.getIsDeleted()) || Boolean.FALSE.equals(p.getActive())) {
                    continue;
                }
                if (p.getStartDate() != null && today.isBefore(p.getStartDate())) {
                    continue;
                }
                if (p.getEndDate() != null && today.isAfter(p.getEndDate())) {
                    continue;
                }
                if (p.getPercentage() != null && p.getPercentage() > 0) {
                    return BigDecimal.valueOf(p.getPercentage()).setScale(4, RoundingMode.HALF_UP);
                }
            }
        } catch (Exception e) {
            log.warn("[Commission] Could not retrieve fallback policy: {}", e.getMessage());
        }

        return DEFAULT_COMMISSION_RATE;
    }

    @Override
    public CommissionPolicy getCommissionPolicyForSeller(Seller seller) {
        if (seller == null) {
            return null;
        }

        // Trace: Seller -> ShopCategory -> CommissionPolicy
        ShopCategory category = seller.getShopCategory();
        if (category == null && seller.getShop() != null) {
            category = seller.getShop().getShopCategory();
        }

        if (category != null && category.getCommissionPolicy() != null) {
            CommissionPolicy policy = category.getCommissionPolicy();
            if (Boolean.TRUE.equals(policy.getActive()) && !Boolean.TRUE.equals(policy.getIsDeleted())) {
                return policy;
            }
        }

        // Secondary trace: Check if Shop has a direct CommissionPolicy override
        if (seller.getShop() != null && seller.getShop().getCommissionPolicy() != null) {
            CommissionPolicy policy = seller.getShop().getCommissionPolicy();
            if (Boolean.TRUE.equals(policy.getActive()) && !Boolean.TRUE.equals(policy.getIsDeleted())) {
                return policy;
            }
        }

        return null;
    }
}

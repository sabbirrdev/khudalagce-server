package com.company.efood.sys.service;

import com.company.efood.sys.entity.Commission;
import com.company.efood.sys.entity.Order;
import com.company.efood.sys.entity.Shop;
import com.company.efood.sys.repository.CommissionPolicyRepo;
import com.company.efood.sys.repository.CommissionRepo;
import com.company.efood.sys.repository.OrderRepo;
import com.company.efood.sys.repository.ShopRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class CommissionEngineService {
    private final CommissionPolicyRepo policyRepo;
    private final CommissionRepo commissionRepo;
    private final OrderRepo orderRepo;
    private final ShopRepo shopRepo;
    private final com.company.efood.sys.services.SellerCommissionCalculationService sellerCommissionCalculationService;

    public Commission calculateAndSave(Long orderId, Long shopId, String shopType, 
                                        BigDecimal productSellingPrice, BigDecimal deliveryFee) {
        Order order = orderRepo.findById(orderId).orElseThrow(() -> new RuntimeException("Order not found"));
        Shop shop = shopRepo.findById(shopId).orElseThrow(() -> new RuntimeException("Shop not found"));

        // Seller commission fee is calculated strictly on product selling price (EXCEPT delivery fee)
        BigDecimal baseSellingPrice = productSellingPrice;
        BigDecimal dFee = deliveryFee != null ? deliveryFee : (order.getDeliveryFee() != null ? order.getDeliveryFee() : BigDecimal.ZERO);

        if (baseSellingPrice == null || baseSellingPrice.compareTo(BigDecimal.ZERO) <= 0) {
            BigDecimal ordTotal = order.getTotalAmount() != null ? order.getTotalAmount() : BigDecimal.ZERO;
            BigDecimal discount = order.getDiscount() != null ? order.getDiscount() : BigDecimal.ZERO;
            BigDecimal tax = order.getTax() != null ? order.getTax() : BigDecimal.ZERO;
            baseSellingPrice = ordTotal.subtract(dFee).subtract(tax).add(discount);
            if (baseSellingPrice.compareTo(BigDecimal.ZERO) < 0) baseSellingPrice = BigDecimal.ZERO;
        } else if (order.getTotalAmount() != null && baseSellingPrice.compareTo(order.getTotalAmount()) == 0 && dFee.compareTo(BigDecimal.ZERO) > 0) {
            // Defensive guard: if caller mistakenly passed order.getTotalAmount(), strip delivery fee
            baseSellingPrice = baseSellingPrice.subtract(dFee);
            if (baseSellingPrice.compareTo(BigDecimal.ZERO) < 0) baseSellingPrice = BigDecimal.ZERO;
        }

        BigDecimal rate = getCommissionRate(order, shop, shopType, baseSellingPrice);
        BigDecimal commissionAmount = baseSellingPrice.multiply(rate)
                .divide(BigDecimal.valueOf(100), 4, java.math.RoundingMode.HALF_UP);
        BigDecimal merchantPayout = baseSellingPrice.subtract(commissionAmount)
                .setScale(4, java.math.RoundingMode.HALF_UP);

        Commission c = new Commission();
        c.setOrder(order);
        c.setShop(shop);
        c.setOrderTotal(baseSellingPrice.setScale(4, java.math.RoundingMode.HALF_UP));
        c.setCommissionRate(rate);
        c.setCommissionAmount(commissionAmount);
        c.setDeliveryFee(dFee.setScale(4, java.math.RoundingMode.HALF_UP));
        c.setMerchantPayout(merchantPayout);
        c.setShopType(shopType);
        c.setStatus("PENDING");
        c.setPaid(false);
        c.setEntryUser(0L);
        c.setEntryDate(java.time.LocalDateTime.now());
        c.setActive(true);
        return commissionRepo.save(c);
    }

    private BigDecimal getCommissionRate(Order order, Shop shop, String shopType, BigDecimal orderTotal) {
        // 0. Primary Trace: Seller -> ShopCategory -> CommissionPolicy
        if (shop != null && shop.getSeller() != null && sellerCommissionCalculationService != null) {
            com.company.efood.sys.entity.CommissionPolicy policy = 
                    sellerCommissionCalculationService.getCommissionPolicyForSeller(shop.getSeller());
            if (policy != null && policy.getPercentage() != null && policy.getPercentage() > 0) {
                return BigDecimal.valueOf(policy.getPercentage()).setScale(4, java.math.RoundingMode.HALF_UP);
            }
        }

        // 1. Check if order has an assigned commission policy
        if (order != null && order.getCommissionPolicy() != null) {
            Double pct = order.getCommissionPolicy().getPercentage();
            if (pct != null && pct > 0) {
                return BigDecimal.valueOf(pct).setScale(4, java.math.RoundingMode.HALF_UP);
            }
        }

        // 2. Query configured policies in CommissionPolicyRepo
        try {
            java.util.List<com.company.efood.sys.entity.CommissionPolicy> policies = policyRepo.findAll();
            if (policies != null && !policies.isEmpty()) {
                // Priority A: policy name matches shopType or shop name
                for (com.company.efood.sys.entity.CommissionPolicy p : policies) {
                    if (Boolean.TRUE.equals(p.getIsDeleted()) || Boolean.FALSE.equals(p.getActive())) {
                        continue;
                    }
                    if (p.getPercentage() != null && p.getPercentage() > 0) {
                        String pName = p.getName() != null ? p.getName().trim().toLowerCase() : "";
                        if (shopType != null && pName.equalsIgnoreCase(shopType.trim())) {
                            return BigDecimal.valueOf(p.getPercentage()).setScale(4, java.math.RoundingMode.HALF_UP);
                        }
                        if (shop != null && shop.getShopName() != null && pName.equalsIgnoreCase(shop.getShopName().trim())) {
                            return BigDecimal.valueOf(p.getPercentage()).setScale(4, java.math.RoundingMode.HALF_UP);
                        }
                    }
                }

                // Priority B: first active, non-deleted policy applicable
                for (com.company.efood.sys.entity.CommissionPolicy p : policies) {
                    if (Boolean.TRUE.equals(p.getIsDeleted()) || Boolean.FALSE.equals(p.getActive())) {
                        continue;
                    }
                    if (p.getPercentage() != null && p.getPercentage() > 0) {
                        // Check order amount bounds if defined
                        if (p.getMinOrderAmount() != null && orderTotal != null && orderTotal.doubleValue() < p.getMinOrderAmount()) {
                            continue;
                        }
                        if (p.getMaxOrderAmount() != null && orderTotal != null && orderTotal.doubleValue() > p.getMaxOrderAmount()) {
                            continue;
                        }
                        return BigDecimal.valueOf(p.getPercentage()).setScale(4, java.math.RoundingMode.HALF_UP);
                    }
                }
            }
        } catch (Exception ignored) {}

        // 3. Fallback default commission rates by shop category
        if (shopType == null) return BigDecimal.valueOf(10.0000).setScale(4, java.math.RoundingMode.HALF_UP);
        return switch (shopType.toUpperCase()) {
            case "RESTAURANT" -> BigDecimal.valueOf(15.0000).setScale(4, java.math.RoundingMode.HALF_UP);
            case "HOME_SERVICE" -> BigDecimal.valueOf(20.0000).setScale(4, java.math.RoundingMode.HALF_UP);
            default -> BigDecimal.valueOf(10.0000).setScale(4, java.math.RoundingMode.HALF_UP); // GROCERY, MEDICINE
        };
    }
}

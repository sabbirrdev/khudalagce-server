package com.company.efood.sys.services.serviceimpl;

import com.company.efood.base.BasePageableRequest;
import com.company.efood.base.BaseUtils;
import com.company.efood.raider.entity.Raider;
import com.company.efood.raider.entity.RaiderEarning;
import com.company.efood.raider.repository.RaiderEarningRepo;
import com.company.efood.sys.dto.OrderFinancialLedgerDto;
import com.company.efood.sys.entity.*;
import com.company.efood.sys.repository.CommissionRepo;
import com.company.efood.sys.repository.OrderFinancialLedgerRepo;
import com.company.efood.user.repository.OrderItemRepo;
import com.company.efood.sys.repository.OrderRepo;
import com.company.efood.sys.service.CommissionEngineService;
import com.company.efood.sys.services.OrderFinancialLedgerService;
import com.company.efood.sys.services.RiderPayoutPolicyService;
import com.company.efood.sys.utils.OrderStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderFinancialLedgerServiceImpl implements OrderFinancialLedgerService {

    private final OrderFinancialLedgerRepo ledgerRepo;
    private final OrderRepo orderRepo;
    private final OrderItemRepo orderItemRepo;
    private final CommissionRepo commissionRepo;
    private final CommissionEngineService commissionEngineService;
    private final RiderPayoutPolicyService riderPayoutPolicyService;
    private final RaiderEarningRepo raiderEarningRepo;
    private final BaseUtils baseUtils;

    @Override
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public OrderFinancialLedger recordOrderCompletion(Order order) {
        if (order == null || order.getId() == null) {
            throw new IllegalArgumentException("Valid order is required to record financial ledger");
        }

        // Reload order with associations
        Order freshOrder = orderRepo.findById(order.getId()).orElse(order);
        Shop shop = freshOrder.getBranch() != null ? freshOrder.getBranch().getShop() : null;
        Raider raider = freshOrder.getRaider();

        BigDecimal customerTotal = freshOrder.getTotalAmount() != null
                ? freshOrder.getTotalAmount().setScale(4, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);

        BigDecimal deliveryFee = freshOrder.getDeliveryFee() != null
                ? freshOrder.getDeliveryFee().setScale(4, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);

        BigDecimal discount = freshOrder.getDiscount() != null
                ? freshOrder.getDiscount().setScale(4, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);

        BigDecimal tax = freshOrder.getTax() != null
                ? freshOrder.getTax().setScale(4, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);

        // Fetch OrderItems
        List<OrderItem> items = freshOrder.getOrderItemList();
        if ((items == null || items.isEmpty()) && freshOrder.getId() != null) {
            items = orderItemRepo.findByOrderId(freshOrder.getId());
        }

        BigDecimal productSubtotal = BigDecimal.ZERO;
        BigDecimal productBuyingCost = BigDecimal.ZERO;

        if (items != null && !items.isEmpty()) {
            for (OrderItem item : items) {
                BigDecimal qty = BigDecimal.valueOf(item.getQuantity() != null ? item.getQuantity() : 1);
                BigDecimal unitPrice = item.getUnitPrice() != null
                        ? item.getUnitPrice()
                        : (item.getProduct() != null && item.getProduct().getPrice() != null ? item.getProduct().getPrice() : BigDecimal.ZERO);
                
                BigDecimal itemSubtotal = unitPrice.multiply(qty);
                productSubtotal = productSubtotal.add(itemSubtotal);

                BigDecimal unitBuyingPrice = item.getBuyingPrice() != null
                        ? item.getBuyingPrice()
                        : (item.getProduct() != null && item.getProduct().getBuyingPrice() != null ? item.getProduct().getBuyingPrice() : BigDecimal.ZERO);
                
                BigDecimal itemCost = unitBuyingPrice.multiply(qty);
                productBuyingCost = productBuyingCost.add(itemCost);
            }
        } else {
            // Fallback product subtotal = customerTotal - deliveryFee + discount - tax
            productSubtotal = customerTotal.subtract(deliveryFee).add(discount).subtract(tax);
            if (productSubtotal.compareTo(BigDecimal.ZERO) < 0) productSubtotal = BigDecimal.ZERO;
        }

        productSubtotal = productSubtotal.setScale(4, RoundingMode.HALF_UP);
        productBuyingCost = productBuyingCost.setScale(4, RoundingMode.HALF_UP);

        // Commission calculation - Strictly on product selling price (EXCEPT delivery fee)
        List<Commission> commissions = commissionRepo.findByOrderId(freshOrder.getId());
        Commission commission = (commissions != null && !commissions.isEmpty()) ? commissions.get(0) : null;
        BigDecimal commissionRate = BigDecimal.valueOf(10.0000).setScale(4, RoundingMode.HALF_UP);
        BigDecimal sellerCommissionAmount;

        if (commission != null) {
            commissionRate = commission.getCommissionRate() != null
                    ? commission.getCommissionRate().setScale(4, RoundingMode.HALF_UP)
                    : commissionRate;
            // Always calculate seller commission strictly on productSubtotal (excluding delivery fee)
            sellerCommissionAmount = productSubtotal.multiply(commissionRate)
                    .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);

            // Sync Commission record to reflect the exact product selling price base
            commission.setOrderTotal(productSubtotal);
            commission.setCommissionAmount(sellerCommissionAmount);
            commission.setMerchantPayout(productSubtotal.subtract(sellerCommissionAmount).setScale(4, RoundingMode.HALF_UP));
            commission.setDeliveryFee(deliveryFee);
            commissionRepo.save(commission);
        } else if (shop != null) {
            String shopType = shop.getShopType() != null ? shop.getShopType() : "RESTAURANT";
            Commission newComm = commissionEngineService.calculateAndSave(freshOrder.getId(), shop.getId(), shopType, productSubtotal, deliveryFee);
            commissionRate = newComm.getCommissionRate().setScale(4, RoundingMode.HALF_UP);
            sellerCommissionAmount = newComm.getCommissionAmount().setScale(4, RoundingMode.HALF_UP);
        } else {
            sellerCommissionAmount = productSubtotal.multiply(commissionRate).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        }

        // Seller Net Earnings = productSubtotal - sellerCommissionAmount
        BigDecimal sellerNetEarnings = productSubtotal.subtract(sellerCommissionAmount).setScale(4, RoundingMode.HALF_UP);
        if (sellerNetEarnings.compareTo(BigDecimal.ZERO) < 0) sellerNetEarnings = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);

        // Rider Share calculation via dynamic Rider Payout Policy
        BigDecimal riderDisbursementAmount = riderPayoutPolicyService.calculateRiderShare(deliveryFee, null)
                .setScale(4, RoundingMode.HALF_UP);

        // Platform Delivery Share = deliveryFee - riderDisbursementAmount
        BigDecimal platformDeliveryShare = deliveryFee.subtract(riderDisbursementAmount).setScale(4, RoundingMode.HALF_UP);
        if (platformDeliveryShare.compareTo(BigDecimal.ZERO) < 0) platformDeliveryShare = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);

        // Net Platform Revenue = sellerCommissionAmount + platformDeliveryShare
        BigDecimal netPlatformRevenue = sellerCommissionAmount.add(platformDeliveryShare).setScale(4, RoundingMode.HALF_UP);

        // Fetch or create ledger entry
        OrderFinancialLedger ledger = ledgerRepo.findByOrder_Id(freshOrder.getId()).orElse(new OrderFinancialLedger());
        ledger.setOrder(freshOrder);
        ledger.setShop(shop);
        ledger.setRaider(raider);
        ledger.setCustomerTotal(customerTotal);
        ledger.setProductSubtotal(productSubtotal);
        ledger.setProductBuyingCost(productBuyingCost);
        ledger.setDiscount(discount);
        ledger.setTax(tax);
        ledger.setDeliveryFee(deliveryFee);
        ledger.setCommissionRate(commissionRate);
        ledger.setSellerCommissionAmount(sellerCommissionAmount);
        ledger.setSellerNetEarnings(sellerNetEarnings);
        ledger.setRiderDisbursementAmount(riderDisbursementAmount);
        ledger.setPlatformDeliveryShare(platformDeliveryShare);
        ledger.setNetPlatformRevenue(netPlatformRevenue);

        if (freshOrder.getPayment() != null) {
            ledger.setPaymentMethod(freshOrder.getPayment().getPaymentMethod() != null ? freshOrder.getPayment().getPaymentMethod().name() : "COD");
            ledger.setPaymentStatus(freshOrder.getPayment().getStatus() != null ? freshOrder.getPayment().getStatus().name() : "PAID");
        } else {
            ledger.setPaymentMethod("COD");
            ledger.setPaymentStatus("PAID");
        }

        ledger.setCompletedAt(freshOrder.getDeliveredAt() != null ? freshOrder.getDeliveredAt() : LocalDateTime.now());
        ledger.setActive(true);
        if (ledger.getId() == null) {
            ledger.setEntryUser(freshOrder.getUpdateUser() != null ? freshOrder.getUpdateUser() : 0L);
            ledger.setEntryDate(LocalDateTime.now());
        } else {
            ledger.setUpdateUser(freshOrder.getUpdateUser() != null ? freshOrder.getUpdateUser() : 0L);
            ledger.setUpdateDate(LocalDateTime.now());
        }

        OrderFinancialLedger savedLedger = ledgerRepo.save(ledger);

        // Also record/sync RaiderEarning
        if (raider != null && raider.getId() != null) {
            try {
                RaiderEarning earning = raiderEarningRepo.findByOrderId(freshOrder.getId()).orElse(new RaiderEarning());
                earning.setRaider(raider);
                earning.setOrderId(freshOrder.getId());
                earning.setEarnedAmount(riderDisbursementAmount.doubleValue());
                earning.setEarnedAt(ledger.getCompletedAt());
                earning.setPaid(false);
                earning.setActive(true);
                if (earning.getId() == null) {
                    earning.setEntryUser(raider.getId());
                    earning.setEntryDate(LocalDateTime.now());
                }
                raiderEarningRepo.save(earning);
            } catch (Exception e) {
                log.warn("Could not save RaiderEarning: {}", e.getMessage());
            }
        }

        return savedLedger;
    }

    @Override
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public void backfillPendingCompletedOrders() {
        try {
            List<Order> completedOrders = orderRepo.findAll().stream()
                    .filter(o -> o.getStatus() == OrderStatus.DELIVERED || o.getStatus() == OrderStatus.COMPLETED)
                    .collect(Collectors.toList());
            for (Order ord : completedOrders) {
                if (ord.getId() != null) {
                    var existingLedger = ledgerRepo.findByOrder_Id(ord.getId());
                    if (existingLedger.isEmpty()) {
                        log.info("[Financial] Auto-backfilling missing ledger entry for completed order #{}", ord.getId());
                        recordOrderCompletion(ord);
                    } else {
                        // Check if seller commission was calculated on delivery fee by verifying productSubtotal * rate / 100
                        OrderFinancialLedger l = existingLedger.get();
                        BigDecimal expectedCommission = l.getProductSubtotal() != null && l.getCommissionRate() != null
                                ? l.getProductSubtotal().multiply(l.getCommissionRate()).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP)
                                : BigDecimal.ZERO;
                        if (l.getSellerCommissionAmount() != null && expectedCommission.compareTo(BigDecimal.ZERO) > 0
                                && l.getSellerCommissionAmount().setScale(2, RoundingMode.HALF_UP).compareTo(expectedCommission.setScale(2, RoundingMode.HALF_UP)) != 0) {
                            log.info("[Financial] Auto-healing ledger seller commission calculation for order #{}", ord.getId());
                            recordOrderCompletion(ord);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("[Financial] Error during order ledger backfill: {}", e.getMessage());
        }
    }

    @Override
    @Transactional
    public Page<OrderFinancialLedgerDto> getLedgerEntries(BasePageableRequest request) {
        backfillPendingCompletedOrders();
        PageRequest pageRequest = baseUtils.getPageRequest(request.getPage(), request.getSize());
        Page<OrderFinancialLedger> page = ledgerRepo.findAllByOrderByCompletedAtDesc(pageRequest);
        List<OrderFinancialLedgerDto> dtos = page.stream().map(this::toDto).collect(Collectors.toList());
        return new PageImpl<>(dtos, pageRequest, page.getTotalElements());
    }

    @Override
    @Transactional
    public Map<String, Object> getFinancialSummary() {
        backfillPendingCompletedOrders();
        BigDecimal totalSellerNet = ledgerRepo.sumTotalSellerNetEarnings().setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalRiderDisbursement = ledgerRepo.sumTotalRiderDisbursement().setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalNetPlatformRevenue = ledgerRepo.sumTotalNetPlatformRevenue().setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalCustomerVolume = ledgerRepo.sumTotalCustomerVolume().setScale(2, RoundingMode.HALF_UP);
        long completedTransactions = ledgerRepo.count();

        Map<String, Object> summary = new HashMap<>();
        summary.put("totalSellerDisbursed", totalSellerNet);
        summary.put("totalRiderDisbursed", totalRiderDisbursement);
        summary.put("netPlatformRevenue", totalNetPlatformRevenue);
        summary.put("totalCustomerVolume", totalCustomerVolume);
        summary.put("completedTransactions", completedTransactions);
        return summary;
    }

    private OrderFinancialLedgerDto toDto(OrderFinancialLedger entity) {
        OrderFinancialLedgerDto dto = new OrderFinancialLedgerDto();
        dto.setId(entity.getId());
        dto.setOrderId(entity.getOrder() != null ? entity.getOrder().getId() : null);
        if (entity.getShop() != null) {
            dto.setShopId(entity.getShop().getId());
            dto.setShopName(entity.getShop().getShopName());
        }
        if (entity.getRaider() != null) {
            dto.setRaiderId(entity.getRaider().getId());
            dto.setRaiderName(entity.getRaider().getFullName() != null ? entity.getRaider().getFullName() : "Rider #" + entity.getRaider().getId());
        }
        dto.setCustomerTotal(entity.getCustomerTotal().setScale(2, RoundingMode.HALF_UP));
        dto.setProductSubtotal(entity.getProductSubtotal().setScale(2, RoundingMode.HALF_UP));
        dto.setProductBuyingCost(entity.getProductBuyingCost().setScale(2, RoundingMode.HALF_UP));
        dto.setDiscount(entity.getDiscount().setScale(2, RoundingMode.HALF_UP));
        dto.setTax(entity.getTax().setScale(2, RoundingMode.HALF_UP));
        dto.setDeliveryFee(entity.getDeliveryFee().setScale(2, RoundingMode.HALF_UP));
        dto.setCommissionRate(entity.getCommissionRate().setScale(2, RoundingMode.HALF_UP));
        dto.setSellerCommissionAmount(entity.getSellerCommissionAmount().setScale(2, RoundingMode.HALF_UP));
        dto.setSellerNetEarnings(entity.getSellerNetEarnings().setScale(2, RoundingMode.HALF_UP));
        dto.setRiderDisbursementAmount(entity.getRiderDisbursementAmount().setScale(2, RoundingMode.HALF_UP));
        dto.setPlatformDeliveryShare(entity.getPlatformDeliveryShare().setScale(2, RoundingMode.HALF_UP));
        dto.setNetPlatformRevenue(entity.getNetPlatformRevenue().setScale(2, RoundingMode.HALF_UP));
        dto.setPaymentMethod(entity.getPaymentMethod());
        dto.setPaymentStatus(entity.getPaymentStatus());
        dto.setCompletedAt(entity.getCompletedAt());
        dto.setActive(entity.getActive());
        return dto;
    }
}

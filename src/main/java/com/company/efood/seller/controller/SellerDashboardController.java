package com.company.efood.seller.controller;

import com.company.efood.base.BaseResponse;
import com.company.efood.base.BaseUtils;
import com.company.efood.config.CurrentUserContext;
import com.company.efood.seller.entity.Seller;
import com.company.efood.seller.repository.SellerRepo;
import com.company.efood.sys.entity.Order;
import com.company.efood.sys.repository.OrderRepo;
import com.company.efood.sys.utils.AuthTokenUtils;
import com.company.efood.sys.utils.OrderStatus;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static com.company.efood.base.BaseConstants.PROCESS_COMPLETE;
import static com.company.efood.base.BaseConstants.PROCESS_COMPLETE_BN;
import static com.company.efood.base.BaseConstants.SELLER_END_POINT;

@Slf4j
@RestController
@RequestMapping({SELLER_END_POINT + "dashboard", "/api/private/seller/dashboard", "/seller/dashboard"})
@RequiredArgsConstructor
public class SellerDashboardController {

    private final OrderRepo orderRepo;
    private final SellerRepo sellerRepo;
    private final com.company.efood.sys.repository.ShopRepo shopRepo;
    private final com.company.efood.user.repository.OrderItemRepo orderItemRepo;
    private final com.company.efood.sys.repository.OrderFinancialLedgerRepo ledgerRepo;
    private final com.company.efood.sys.services.OrderFinancialLedgerService financialLedgerService;
    private final BaseUtils baseUtils;
    private final AuthTokenUtils authTokenUtils;

    @GetMapping("/metrics")
    public ResponseEntity<BaseResponse> getSellerMetrics(HttpServletRequest request) {
        try {
            Long rawUserId = authTokenUtils.getUserIdFromRequest(request);
            if (rawUserId == null) {
                rawUserId = CurrentUserContext.getReferenceId();
            }
            final Long finalUserId = rawUserId;

            // Auto-backfill ledger if any completed order is missing
            try {
                financialLedgerService.backfillPendingCompletedOrders();
            } catch (Exception ignored) {}

            Seller seller = sellerRepo.findByAppUserId(finalUserId)
                    .orElseGet(() -> sellerRepo.findById(finalUserId).orElse(null));
            Long foundSellerId = seller != null ? seller.getId() : null;
            Long foundAppUserId = seller != null && seller.getAppUser() != null ? seller.getAppUser().getId() : finalUserId;

            // Collect all Shop IDs associated with this seller
            Set<Long> sellerShopIds = new HashSet<>();
            if (seller != null) {
                if (seller.getShop() != null && seller.getShop().getId() != null) {
                    sellerShopIds.add(seller.getShop().getId());
                }
                shopRepo.findShopBySellerId(seller.getId()).ifPresent(s -> sellerShopIds.add(s.getId()));
            }
            if (sellerShopIds.isEmpty()) {
                shopRepo.findAll().stream()
                        .filter(s -> (s.getSeller() != null && (Objects.equals(s.getSeller().getId(), foundSellerId) ||
                                (s.getSeller().getAppUser() != null && Objects.equals(s.getSeller().getAppUser().getId(), foundAppUserId)))))
                        .forEach(s -> sellerShopIds.add(s.getId()));
            }

            List<Order> allSellerOrders = orderRepo.findAll().stream()
                    .filter(o -> {
                        if (o.getBranch() != null && o.getBranch().getShop() != null) {
                            Long shopId = o.getBranch().getShop().getId();
                            if (sellerShopIds.contains(shopId)) return true;
                            Seller s = o.getBranch().getShop().getSeller();
                            if (s != null) {
                                if (foundSellerId != null && Objects.equals(s.getId(), foundSellerId)) return true;
                                if (s.getAppUser() != null && Objects.equals(s.getAppUser().getId(), foundAppUserId)) return true;
                            }
                        }
                        return false;
                    })
                    .collect(Collectors.toList());

            long totalOrders = allSellerOrders.size();
            List<Order> completedOrderList = allSellerOrders.stream()
                    .filter(o -> o.getStatus() == OrderStatus.COMPLETED || o.getStatus() == OrderStatus.DELIVERED)
                    .collect(Collectors.toList());
            long completedOrders = completedOrderList.size();
            long activeOrders = allSellerOrders.stream().filter(o -> o.getStatus() == OrderStatus.PLACED || o.getStatus() == OrderStatus.ACCEPTED || o.getStatus() == OrderStatus.PREPARING || o.getStatus() == OrderStatus.ASSIGNED || o.getStatus() == OrderStatus.PICKED_UP || o.getStatus() == OrderStatus.ON_THE_WAY).count();
            long cancelledOrders = allSellerOrders.stream().filter(o -> o.getStatus() == OrderStatus.CANCELLED || o.getStatus() == OrderStatus.REJECTED).count();

            // Check if we have OrderFinancialLedger records for precise bookkeeping
            List<com.company.efood.sys.entity.OrderFinancialLedger> sellerLedgers = ledgerRepo.findAll().stream()
                    .filter(l -> {
                        if (l.getShop() != null) {
                            Long shopId = l.getShop().getId();
                            if (sellerShopIds.contains(shopId)) return true;
                            Seller s = l.getShop().getSeller();
                            if (s != null) {
                                if (foundSellerId != null && Objects.equals(s.getId(), foundSellerId)) return true;
                                if (s.getAppUser() != null && Objects.equals(s.getAppUser().getId(), foundAppUserId)) return true;
                            }
                        }
                        return false;
                    })
                    .collect(Collectors.toList());

            BigDecimal totalRevenue = BigDecimal.ZERO;
            BigDecimal totalBuyingCost = BigDecimal.ZERO;
            BigDecimal platformCommission = BigDecimal.ZERO;
            BigDecimal sellerNetEarnings = BigDecimal.ZERO;

            if (!sellerLedgers.isEmpty()) {
                totalRevenue = sellerLedgers.stream().map(com.company.efood.sys.entity.OrderFinancialLedger::getProductSubtotal).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
                totalBuyingCost = sellerLedgers.stream().map(com.company.efood.sys.entity.OrderFinancialLedger::getProductBuyingCost).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
                platformCommission = sellerLedgers.stream().map(com.company.efood.sys.entity.OrderFinancialLedger::getSellerCommissionAmount).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
                sellerNetEarnings = sellerLedgers.stream().map(com.company.efood.sys.entity.OrderFinancialLedger::getSellerNetEarnings).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
            } else {
                for (Order ord : completedOrderList) {
                    BigDecimal ordProductSubtotal = BigDecimal.ZERO;
                    List<com.company.efood.sys.entity.OrderItem> items = ord.getOrderItemList();
                    if ((items == null || items.isEmpty()) && ord.getId() != null) {
                        items = orderItemRepo.findByOrderId(ord.getId());
                    }
                    if (items != null && !items.isEmpty()) {
                        for (com.company.efood.sys.entity.OrderItem item : items) {
                            BigDecimal qty = BigDecimal.valueOf(item.getQuantity() != null ? item.getQuantity() : 1);
                            BigDecimal uPrice = item.getUnitPrice() != null
                                    ? item.getUnitPrice()
                                    : (item.getProduct() != null && item.getProduct().getPrice() != null ? item.getProduct().getPrice() : BigDecimal.ZERO);
                            ordProductSubtotal = ordProductSubtotal.add(uPrice.multiply(qty));

                            BigDecimal bPrice = item.getBuyingPrice() != null
                                    ? item.getBuyingPrice()
                                    : (item.getProduct() != null && item.getProduct().getBuyingPrice() != null ? item.getProduct().getBuyingPrice() : BigDecimal.ZERO);
                            totalBuyingCost = totalBuyingCost.add(bPrice.multiply(qty));
                        }
                    } else {
                        // Fallback: ord.getTotalAmount() - deliveryFee - tax + discount
                        BigDecimal ordTot = ord.getTotalAmount() != null ? ord.getTotalAmount() : BigDecimal.ZERO;
                        BigDecimal dFee = ord.getDeliveryFee() != null ? ord.getDeliveryFee() : BigDecimal.ZERO;
                        BigDecimal tax = ord.getTax() != null ? ord.getTax() : BigDecimal.ZERO;
                        BigDecimal disc = ord.getDiscount() != null ? ord.getDiscount() : BigDecimal.ZERO;
                        ordProductSubtotal = ordTot.subtract(dFee).subtract(tax).add(disc);
                        if (ordProductSubtotal.compareTo(BigDecimal.ZERO) < 0) ordProductSubtotal = BigDecimal.ZERO;
                    }
                    totalRevenue = totalRevenue.add(ordProductSubtotal);
                }

                totalRevenue = totalRevenue.setScale(2, RoundingMode.HALF_UP);
                totalBuyingCost = totalBuyingCost.setScale(2, RoundingMode.HALF_UP);
                // Seller commission fee is calculated ONLY on product selling price (EXCEPT delivery fee)
                platformCommission = totalRevenue.multiply(BigDecimal.valueOf(0.10)).setScale(2, RoundingMode.HALF_UP);
                sellerNetEarnings = totalRevenue.subtract(platformCommission).setScale(2, RoundingMode.HALF_UP);
            }

            // Gross Profit = Selling Price (Product Revenue) - Buying Price (COGS)
            BigDecimal grossProfit = totalRevenue.subtract(totalBuyingCost).setScale(2, RoundingMode.HALF_UP);
            if (grossProfit.compareTo(BigDecimal.ZERO) < 0) grossProfit = BigDecimal.ZERO;

            // Net Profit = Gross Profit - Platform Commission
            BigDecimal netProfit = grossProfit.subtract(platformCommission).setScale(2, RoundingMode.HALF_UP);
            if (netProfit.compareTo(BigDecimal.ZERO) < 0) netProfit = BigDecimal.ZERO;

            BigDecimal grossMarginPct = totalRevenue.compareTo(BigDecimal.ZERO) > 0
                    ? grossProfit.multiply(BigDecimal.valueOf(100)).divide(totalRevenue, 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            BigDecimal netMarginPct = totalRevenue.compareTo(BigDecimal.ZERO) > 0
                    ? netProfit.multiply(BigDecimal.valueOf(100)).divide(totalRevenue, 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            BigDecimal averageOrderValue = completedOrders > 0
                    ? totalRevenue.divide(BigDecimal.valueOf(completedOrders), 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            // 7-day revenue and order timeline for charts (Product Selling Price only, excluding delivery fee)
            List<Map<String, Object>> weeklyChart = new ArrayList<>();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd");

            for (int i = 6; i >= 0; i--) {
                LocalDate date = LocalDate.now().minusDays(i);
                LocalDateTime dayStart = date.atStartOfDay();
                LocalDateTime dayEnd = dayStart.plusDays(1).minusNanos(1);

                List<Order> dayOrders = allSellerOrders.stream()
                        .filter(o -> o.getEntryDate() != null && !o.getEntryDate().isBefore(dayStart) && !o.getEntryDate().isAfter(dayEnd))
                        .collect(Collectors.toList());

                BigDecimal dayRevenue = BigDecimal.ZERO;
                for (Order o : dayOrders) {
                    if (o.getStatus() == OrderStatus.COMPLETED || o.getStatus() == OrderStatus.DELIVERED) {
                        BigDecimal dFee = o.getDeliveryFee() != null ? o.getDeliveryFee() : BigDecimal.ZERO;
                        BigDecimal tot = o.getTotalAmount() != null ? o.getTotalAmount() : BigDecimal.ZERO;
                        BigDecimal prodRev = tot.subtract(dFee);
                        if (prodRev.compareTo(BigDecimal.ZERO) > 0) {
                            dayRevenue = dayRevenue.add(prodRev);
                        }
                    }
                }
                dayRevenue = dayRevenue.setScale(2, RoundingMode.HALF_UP);

                Map<String, Object> point = new HashMap<>();
                point.put("date", date.format(formatter));
                point.put("revenue", dayRevenue);
                point.put("orders", dayOrders.size());
                point.put("completed", dayOrders.stream().filter(o -> o.getStatus() == OrderStatus.COMPLETED || o.getStatus() == OrderStatus.DELIVERED).count());
                weeklyChart.add(point);
            }

            Map<String, Object> metrics = new HashMap<>();
            metrics.put("totalRevenue", totalRevenue);
            metrics.put("sellerNetEarnings", sellerNetEarnings);
            metrics.put("totalBuyingCost", totalBuyingCost);
            metrics.put("grossProfit", grossProfit);
            metrics.put("netProfit", netProfit);
            metrics.put("platformCommission", platformCommission);
            metrics.put("grossMarginPct", grossMarginPct);
            metrics.put("netMarginPct", netMarginPct);
            metrics.put("averageOrderValue", averageOrderValue);
            metrics.put("totalOrders", totalOrders);
            metrics.put("completedOrders", completedOrders);
            metrics.put("activeOrders", activeOrders);
            metrics.put("cancelledOrders", cancelledOrders);
            metrics.put("weeklyChart", weeklyChart);

            return ResponseEntity.ok(baseUtils.generateSuccessResponse(metrics, PROCESS_COMPLETE, PROCESS_COMPLETE_BN));
        } catch (Exception e) {
            log.error("Error generating seller metrics", e);
            return ResponseEntity.badRequest().body(baseUtils.generateErrorResponse(e));
        }
    }
}

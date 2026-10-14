package com.company.efood.sys.entity;

import com.company.efood.base.BaseEntity;
import com.company.efood.raider.entity.Raider;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "ORDER_FINANCIAL_LEDGER", indexes = {
        @Index(name = "idx_ledger_order_id", columnList = "ORDER_ID", unique = true),
        @Index(name = "idx_ledger_completed_at", columnList = "COMPLETED_AT")
})
@Data
@EqualsAndHashCode(callSuper = true)
public class OrderFinancialLedger extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ORDER_ID", nullable = false, unique = true)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "SHOP_ID")
    private Shop shop;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "RAIDER_ID")
    private Raider raider;

    // Gross customer transaction value
    @Column(name = "CUSTOMER_TOTAL", precision = 19, scale = 4, nullable = false)
    private BigDecimal customerTotal;

    // Gross selling value of products
    @Column(name = "PRODUCT_SUBTOTAL", precision = 19, scale = 4, nullable = false)
    private BigDecimal productSubtotal;

    // Total Buying Price / COGS of products
    @Column(name = "PRODUCT_BUYING_COST", precision = 19, scale = 4)
    private BigDecimal productBuyingCost = BigDecimal.ZERO;

    @Column(name = "DISCOUNT", precision = 19, scale = 4)
    private BigDecimal discount = BigDecimal.ZERO;

    @Column(name = "TAX", precision = 19, scale = 4)
    private BigDecimal tax = BigDecimal.ZERO;

    @Column(name = "DELIVERY_FEE", precision = 19, scale = 4, nullable = false)
    private BigDecimal deliveryFee;

    // Commission deducted from seller
    @Column(name = "COMMISSION_RATE", precision = 8, scale = 4)
    private BigDecimal commissionRate;

    @Column(name = "SELLER_COMMISSION_AMOUNT", precision = 19, scale = 4, nullable = false)
    private BigDecimal sellerCommissionAmount;

    // 1. Total amount disbursed to Seller (Net product earnings)
    @Column(name = "SELLER_NET_EARNINGS", precision = 19, scale = 4, nullable = false)
    private BigDecimal sellerNetEarnings;

    // 2. Total amount disbursed to Rider (Calculated via active Rider Policy)
    @Column(name = "RIDER_DISBURSEMENT_AMOUNT", precision = 19, scale = 4, nullable = false)
    private BigDecimal riderDisbursementAmount;

    // Remaining delivery share retained by platform
    @Column(name = "PLATFORM_DELIVERY_SHARE", precision = 19, scale = 4, nullable = false)
    private BigDecimal platformDeliveryShare;

    // 3. Net Platform Revenue retained by Admin (Commission + Delivery share)
    @Column(name = "NET_PLATFORM_REVENUE", precision = 19, scale = 4, nullable = false)
    private BigDecimal netPlatformRevenue;

    @Column(name = "PAYMENT_METHOD")
    private String paymentMethod;

    @Column(name = "PAYMENT_STATUS")
    private String paymentStatus;

    @Column(name = "COMPLETED_AT", nullable = false)
    private LocalDateTime completedAt;
}

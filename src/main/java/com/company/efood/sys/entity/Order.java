package com.company.efood.sys.entity;

import com.company.efood.base.BaseEntity;
import com.company.efood.raider.entity.Raider;
import com.company.efood.sys.utils.OrderStatus;
import com.company.efood.user.entity.Customer;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Entity
@Table(name = "ORDERS",indexes = {
        @Index(name = "idx_orders_customer_active_entrydate", columnList = "CUSTOMER_ID, ACTIVE, ENTRY_DATE DESC")
})
@EqualsAndHashCode(callSuper = true)
public class Order extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CUSTOMER_ID", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "BRANCH_ID", nullable = false)
    private Branch branch;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> orderItemList;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "COUPON_ID")
    private Coupon coupon;

    @OnDelete(action = OnDeleteAction.CASCADE)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ADDRESS_ID", nullable = false)
    private Address deliveryAddress;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false)
    private OrderStatus status = OrderStatus.PLACED;

    @Column(name = "TOTAL_AMOUNT", nullable = false)
    private BigDecimal totalAmount;

    @Column(name = "DISCOUNT")
    private BigDecimal discount = BigDecimal.valueOf(0.0);

    @Column(name = "TAX")
    private BigDecimal tax = BigDecimal.valueOf(0.0);

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ZONE_ID")
    private com.company.efood.zone.entity.Zone zone;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "COMMISSION_POLICY_ID")
    private CommissionPolicy commissionPolicy;

    @Column(name = "DELIVERY_FEE")
    private BigDecimal deliveryFee = new BigDecimal("0.0");

    @OneToOne(mappedBy = "order", cascade = CascadeType.ALL)
    private Payment payment;

    @Column(name = "PREPARATION_TIME")
    private Integer preparationTime; // in minutes

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "RAIDER_ID")
    private Raider raider;

    @Column(name = "TRACKING_LAT")
    private Double trackingLat;

    @Column(name = "TRACKING_LNG")
    private Double trackingLng;

    @Column(name = "DELIVERED_AT")
    private LocalDateTime deliveredAt;
}


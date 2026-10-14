package com.company.efood.sys.dto;

import com.company.efood.base.BaseDto;
import com.company.efood.raider.entity.Raider;
import com.company.efood.sys.entity.*;
import com.company.efood.sys.utils.OrderStatus;
import com.company.efood.user.entity.Customer;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class OrderDto extends BaseDto {
    private Long customerId;
    private String customerName;
    private String customerPhone;
    private Long branchId;
    private OrderStatus status;
    private AddressDto deliveryAddress;
    private Long raiderId;
    private String vehicleType;
    private Double trackingLat;
    private Double trackingLng;
    private Long paymentId;
    private String paymentMethod = "COD";
    private List<OrderItemDto> items;
    private Long couponId;
    private BigDecimal totalAmount;
    private BigDecimal discount = BigDecimal.valueOf(0.0);
    private BigDecimal tax = BigDecimal.valueOf(0.0);
    private Long zoneId;
    private String zoneName;
    private Long commissionPolicyId;
    private BigDecimal deliveryFee;
    private BigDecimal riderEarnings;
    //private Integer preparationTime; // in minutes
    private LocalDateTime deliveredAt;
    private Integer loyaltyPointsToRedeem;
    private BigDecimal loyaltyDiscount;

    // Vendor and Service (Vertical) Metadata for frontend & rider context
    private Long shopId;
    private String shopName;
    private Long shopCategoryId;
    private String shopCategoryName;
    private Long serviceId;
    private String serviceName;
    private String serviceCode;
}

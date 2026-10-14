package com.company.efood.sys.dto;

import com.company.efood.base.BaseDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
public class OrderFinancialLedgerDto extends BaseDto {
    private Long orderId;
    private Long shopId;
    private String shopName;
    private Long raiderId;
    private String raiderName;
    private BigDecimal customerTotal;
    private BigDecimal productSubtotal;
    private BigDecimal productBuyingCost;
    private BigDecimal discount;
    private BigDecimal tax;
    private BigDecimal deliveryFee;
    private BigDecimal commissionRate;
    private BigDecimal sellerCommissionAmount;
    private BigDecimal sellerNetEarnings;
    private BigDecimal riderDisbursementAmount;
    private BigDecimal platformDeliveryShare;
    private BigDecimal netPlatformRevenue;
    private String paymentMethod;
    private String paymentStatus;
    private LocalDateTime completedAt;
}

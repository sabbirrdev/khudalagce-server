package com.company.efood.sys.dto;

import com.company.efood.base.BaseDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
public class PayoutDisbursementDto extends BaseDto {
    private Long beneficiaryUserId;
    private String beneficiaryName;
    private String beneficiaryType; // "SELLER" or "RAIDER"
    private String weekLabel;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private BigDecimal totalEarnings;
    private BigDecimal amount;
    private String payoutMethod;
    private String payoutDestination;
    private String status; // "PENDING", "DISBURSED", "FAILED"
    private String transactionRef;
    private LocalDateTime disbursedAt;
    private Long disbursedBy;
    private String notes;

    public BigDecimal getTotalEarnings() {
        return totalEarnings != null ? totalEarnings : amount;
    }
}

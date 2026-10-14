package com.company.efood.sys.entity;

import com.company.efood.base.BaseEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "PAYOUT_DISBURSEMENT")
@Data
@EqualsAndHashCode(callSuper = true)
public class PayoutDisbursement extends BaseEntity {

    @Column(name = "BENEFICIARY_USER_ID", nullable = false)
    private Long beneficiaryUserId;

    @Column(name = "BENEFICIARY_NAME")
    private String beneficiaryName;

    @Column(name = "BENEFICIARY_TYPE", nullable = false)
    private String beneficiaryType; // "SELLER" or "RAIDER"

    @Column(name = "WEEK_LABEL")
    private String weekLabel; // e.g. "2026-W40"

    @Column(name = "PERIOD_START")
    private LocalDate periodStart;

    @Column(name = "PERIOD_END")
    private LocalDate periodEnd;

    @Column(name = "TOTAL_EARNINGS", precision = 19, scale = 4, nullable = false)
    private BigDecimal totalEarnings;

    @Column(name = "PAYOUT_METHOD")
    private String payoutMethod;

    @Column(name = "PAYOUT_DESTINATION")
    private String payoutDestination;

    @Column(name = "STATUS", nullable = false)
    private String status = "DISBURSED"; // "PENDING", "DISBURSED", "FAILED"

    @Column(name = "TRANSACTION_REF")
    private String transactionRef;

    @Column(name = "DISBURSED_AT")
    private LocalDateTime disbursedAt;

    @Column(name = "DISBURSED_BY")
    private Long disbursedBy;

    @Column(name = "NOTES", columnDefinition = "text")
    private String notes;
}

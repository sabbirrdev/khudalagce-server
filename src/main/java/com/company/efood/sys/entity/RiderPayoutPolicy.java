package com.company.efood.sys.entity;

import com.company.efood.base.BaseEntity;
import com.company.efood.sys.utils.RiderPolicyType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Entity
@Table(name = "RIDER_PAYOUT_POLICY")
@Data
@EqualsAndHashCode(callSuper = true)
public class RiderPayoutPolicy extends BaseEntity {

    @Column(name = "NAME", nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "POLICY_TYPE", nullable = false)
    private RiderPolicyType policyType = RiderPolicyType.PERCENTAGE_SPLIT;

    // Percentage Split configuration
    @Column(name = "RIDER_PERCENTAGE", precision = 8, scale = 4)
    private BigDecimal riderPercentage = new BigDecimal("80.0000");

    @Column(name = "ADMIN_PERCENTAGE", precision = 8, scale = 4)
    private BigDecimal adminPercentage = new BigDecimal("20.0000");

    // Tiered structure configuration
    @Column(name = "BASE_PAYOUT", precision = 12, scale = 4)
    private BigDecimal basePayout = new BigDecimal("40.0000");

    @Column(name = "PER_KM_RATE", precision = 12, scale = 4)
    private BigDecimal perKmRate = new BigDecimal("10.0000");

    @Column(name = "TIER_CONFIG", columnDefinition = "text")
    private String tierConfig;

    @Column(name = "IS_DEFAULT", columnDefinition = "boolean default false")
    private Boolean isDefault = true;

    @Column(name = "DESCRIPTION", columnDefinition = "text")
    private String description;
}

package com.company.efood.sys.dto;

import com.company.efood.base.BaseDto;
import com.company.efood.sys.utils.RiderPolicyType;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
public class RiderPayoutPolicyDto extends BaseDto {
    private String name;
    private String policyName;
    private RiderPolicyType policyType = RiderPolicyType.PERCENTAGE_SPLIT;
    private BigDecimal riderPercentage = new BigDecimal("80.0000");
    private BigDecimal adminPercentage = new BigDecimal("20.0000");
    private BigDecimal basePayout = new BigDecimal("40.0000");
    private BigDecimal perKmRate = new BigDecimal("10.0000");
    private String tierConfig;
    private Boolean isDefault = true;
    private String description;

    public String getName() {
        return name != null && !name.isBlank() ? name : policyName;
    }

    public String getPolicyName() {
        return policyName != null && !policyName.isBlank() ? policyName : name;
    }
}

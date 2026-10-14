package com.company.efood.sys.dto;

import com.company.efood.base.BaseDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
public class ShopCategoryDto extends BaseDto {

    private String name;
    private String nameBn;
    private String code;
    private String description;
    private String icon;
    private String imageUrl;

    // Service relation
    private Long serviceId;
    private String serviceName;
    private String serviceCode;

    // Commission Policy relation
    private Long commissionPolicyId;
    private String commissionPolicyName;
    private Double commissionPercentage;
    private Double commissionFixedAmount;
}

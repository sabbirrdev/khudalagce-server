package com.company.efood.sys.dto;

import com.company.efood.base.BaseDto;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
public class AppServiceDto extends BaseDto {

    private String name;
    private String nameBn;
    private String code;
    private String description;
    private String icon;
    private String color;
    private String gradientColors;
    private Integer displayOrder = 0;
    private Boolean isAlwaysOpen = true;

    @JsonProperty("categories")
    private List<ShopCategoryDto> shopCategories;

    private Integer categoryCount = 0;
}

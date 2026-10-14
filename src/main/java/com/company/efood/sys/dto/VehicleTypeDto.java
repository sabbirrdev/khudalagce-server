package com.company.efood.sys.dto;

import com.company.efood.base.BaseDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class VehicleTypeDto extends BaseDto {
    private String name;
    private String code;
    private String description;
    private List<String> requiredDocuments = new ArrayList<>();
}

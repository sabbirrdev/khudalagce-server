package com.company.efood.raider.dto;

import com.company.efood.base.BaseDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class RaiderEarningDto extends BaseDto {
    private Integer raiderId;
    private Integer orderId;
    private Double earnedAmount;
    private Boolean paid;
    private LocalDateTime earnedAt;
}

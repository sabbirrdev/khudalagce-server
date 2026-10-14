package com.company.efood.sys.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryFeePreviewRequestDto {
    private Long branchId;
    private Long addressId;
    private Long deliveryAddressId;

    /**
     * Total weight of all order items in grams.
     * 0 or null = no weight surcharge applied.
     */
    private Integer totalWeightGrams;

    public Long getEffectiveAddressId() {
        return addressId != null ? addressId : deliveryAddressId;
    }

    public int getEffectiveWeightGrams() {
        return totalWeightGrams != null ? totalWeightGrams : 0;
    }
}

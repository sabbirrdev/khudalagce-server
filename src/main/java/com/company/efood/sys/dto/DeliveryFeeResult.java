package com.company.efood.sys.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryFeeResult {
    private BigDecimal fee;
    private Double distanceKm;
    private String basis; // "HUB_DISTANCE", "HUB_DISTANCE_WEIGHT", "BASE_NO_COORDS", "BASE_NO_COORDS_WEIGHT", "ZONE", "NONE"
    private boolean deliverable;
    private String reason;
    private Long zoneId;
    private String zoneName;

    /** Total order weight used in calculation (grams) */
    private Integer totalWeightGrams;
    /** Weight surcharge amount applied */
    private BigDecimal weightSurcharge;
    /** Distance surcharge amount applied */
    private BigDecimal distanceSurcharge;
    /** Base fee before distance/weight surcharges */
    private BigDecimal baseFee;
}

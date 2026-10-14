package com.company.efood.sys.services;

import com.company.efood.sys.dto.DeliveryFeeResult;

public interface DeliveryFeeCalculationService {

    /**
     * Calculate delivery fee using branch GPS / shop hub location, distance to delivery address,
     * and total product weight in grams.
     *
     * @param branchId          ID of the vendor branch (provides shop GPS)
     * @param deliveryAddressId ID of the customer delivery address (provides GPS + zone info)
     * @param totalWeightGrams  Total weight of all order items in grams (0 = unknown/no weight surcharge)
     */
    DeliveryFeeResult calculate(Long branchId, Long deliveryAddressId, int totalWeightGrams);

    /**
     * Overload without weight (defaults to 0g — no weight surcharge applied).
     */
    default DeliveryFeeResult calculate(Long branchId, Long deliveryAddressId) {
        return calculate(branchId, deliveryAddressId, 0);
    }
}

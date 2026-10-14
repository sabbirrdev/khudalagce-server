package com.company.efood.user.controller;

import com.company.efood.base.BaseResponse;
import com.company.efood.base.BaseUtils;
import com.company.efood.sys.dto.DeliveryFeePreviewRequestDto;
import com.company.efood.sys.dto.DeliveryFeeResult;
import com.company.efood.sys.services.DeliveryFeeCalculationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import static com.company.efood.base.BaseConstants.PRIVET_ENDPOINT;

@RestController
@RequiredArgsConstructor
public class DeliveryFeeController {

    private final DeliveryFeeCalculationService deliveryFeeCalculationService;
    private final BaseUtils baseUtils;

    @PostMapping(PRIVET_ENDPOINT + "delivery-fee/preview")
    public BaseResponse previewDeliveryFee(@RequestBody DeliveryFeePreviewRequestDto request) {
        try {
            Long addressId = request.getEffectiveAddressId();
            DeliveryFeeResult result = deliveryFeeCalculationService.calculate(
                    request.getBranchId(), addressId, request.getEffectiveWeightGrams());
            return baseUtils.generateSuccessResponse(
                    result,
                    "Delivery fee calculated successfully",
                    "ডেলিভারি চার্জ নির্ধারণ করা হয়েছে"
            );
        } catch (Exception ex) {
            return baseUtils.generateErrorResponse(ex);
        }
    }
}

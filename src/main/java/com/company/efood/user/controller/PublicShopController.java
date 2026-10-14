package com.company.efood.user.controller;

import com.company.efood.base.BasePageableRequest;
import com.company.efood.base.BaseResponse;
import com.company.efood.base.BaseUtils;
import com.company.efood.seller.services.BranchService;
import com.company.efood.sys.services.ShopService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

import static com.company.efood.base.BaseConstants.*;

@AllArgsConstructor
@RestController
@RequestMapping(PUBLIC_ENDPOINT+"shop")
public class PublicShopController {

    private final BranchService branchService;
    private final ShopService shopService;
    private final BaseUtils baseUtils;

    @PostMapping("/list")
    public BaseResponse getAllPageableData(@Valid @RequestBody BasePageableRequest basePageableRequest) {
        try {
            return baseUtils.generateSuccessResponse(shopService.getPageableAllData(basePageableRequest, 0L), PROCESS_COMPLETE, PROCESS_COMPLETE_BN);
        } catch (Exception e) {
            return baseUtils.generateErrorResponse(e);
        }
    }

    @GetMapping("/branches/{shopId}")
    public BaseResponse getBranches(@PathVariable Long shopId) {
        try {
            return baseUtils.generateSuccessResponse(branchService.getDropdownListByShopId(shopId, 0L), PROCESS_COMPLETE, PROCESS_COMPLETE_BN);
        } catch (Exception e) {
            return baseUtils.generateErrorResponse(e);
        }
    }

    @GetMapping("/by-service/{serviceId}")
    public BaseResponse getShopsByServiceId(@PathVariable Long serviceId) {
        try {
            return baseUtils.generateSuccessResponse(shopService.getShopsByServiceId(serviceId), PROCESS_COMPLETE, PROCESS_COMPLETE_BN);
        } catch (Exception e) {
            return baseUtils.generateErrorResponse(e);
        }
    }

    @PostMapping("/by-service/{serviceId}")
    public BaseResponse getShopsByServiceIdPageable(
            @PathVariable Long serviceId,
            @Valid @RequestBody BasePageableRequest basePageableRequest) {
        try {
            return baseUtils.generateSuccessResponse(shopService.getShopsByServiceIdPageable(serviceId, basePageableRequest), PROCESS_COMPLETE, PROCESS_COMPLETE_BN);
        } catch (Exception e) {
            return baseUtils.generateErrorResponse(e);
        }
    }
}

package com.company.efood.sys.controller;

import com.company.efood.base.BaseResponse;
import com.company.efood.base.BaseUtils;
import com.company.efood.sys.dto.RiderPayoutPolicyDto;
import com.company.efood.sys.services.RiderPayoutPolicyService;
import com.company.efood.sys.utils.AuthTokenUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.company.efood.base.BaseConstants.*;

@RestController
@RequestMapping(SYSTEM_ADMIN_END_POINT + "rider-payout-policy")
@RequiredArgsConstructor
public class RiderPayoutPolicyController {

    private final RiderPayoutPolicyService policyService;
    private final BaseUtils baseUtils;
    private final AuthTokenUtils authTokenUtils;

    @GetMapping
    public BaseResponse getAllPolicies() {
        try {
            List<RiderPayoutPolicyDto> list = policyService.getAllPolicies();
            return baseUtils.generateSuccessResponse(list, PROCESS_COMPLETE, PROCESS_COMPLETE_BN);
        } catch (Exception e) {
            return baseUtils.generateErrorResponse(e);
        }
    }

    @GetMapping("/active")
    public BaseResponse getActivePolicy() {
        try {
            RiderPayoutPolicyDto active = policyService.getActivePolicy();
            return baseUtils.generateSuccessResponse(active, PROCESS_COMPLETE, PROCESS_COMPLETE_BN);
        } catch (Exception e) {
            return baseUtils.generateErrorResponse(e);
        }
    }

    @GetMapping({"/get-by-id/{id}", "/{id}"})
    public BaseResponse getPolicyById(@PathVariable Long id) {
        try {
            RiderPayoutPolicyDto dto = policyService.getPolicyById(id);
            return baseUtils.generateSuccessResponse(dto, PROCESS_COMPLETE, PROCESS_COMPLETE_BN);
        } catch (Exception e) {
            return baseUtils.generateErrorResponse(e);
        }
    }

    @PutMapping(value = "/pageable-data")
    public BaseResponse getPageableDataPut(@RequestBody com.company.efood.base.BasePageableRequest request) {
        try {
            org.springframework.data.domain.Page<RiderPayoutPolicyDto> page = policyService.getPageableAllData(request);
            return baseUtils.generateSuccessResponse(page, PROCESS_COMPLETE, PROCESS_COMPLETE_BN);
        } catch (Exception e) {
            return baseUtils.generateErrorResponse(e);
        }
    }

    @PostMapping(value = "/pageable-data")
    public BaseResponse getPageableDataPost(@RequestBody com.company.efood.base.BasePageableRequest request) {
        try {
            org.springframework.data.domain.Page<RiderPayoutPolicyDto> page = policyService.getPageableAllData(request);
            return baseUtils.generateSuccessResponse(page, PROCESS_COMPLETE, PROCESS_COMPLETE_BN);
        } catch (Exception e) {
            return baseUtils.generateErrorResponse(e);
        }
    }

    @PostMapping
    public BaseResponse createPolicy(@RequestBody RiderPayoutPolicyDto body, HttpServletRequest request) {
        try {
            Long userId = authTokenUtils.getUserIdFromRequest(request);
            RiderPayoutPolicyDto created = policyService.createPolicy(body, userId);
            return baseUtils.generateSuccessResponse(created, SAVE_MESSAGE, SAVE_MESSAGE_BN);
        } catch (Exception e) {
            return baseUtils.generateErrorResponse(e);
        }
    }

    @PutMapping("/{id}")
    public BaseResponse updatePolicyWithId(@PathVariable Long id, @RequestBody RiderPayoutPolicyDto body, HttpServletRequest request) {
        try {
            Long userId = authTokenUtils.getUserIdFromRequest(request);
            RiderPayoutPolicyDto updated = policyService.updatePolicy(id, body, userId);
            return baseUtils.generateSuccessResponse(updated, UPDATE_MESSAGE, UPDATE_MESSAGE_BN);
        } catch (Exception e) {
            return baseUtils.generateErrorResponse(e);
        }
    }

    @PutMapping
    public BaseResponse updatePolicyFromBody(@RequestBody RiderPayoutPolicyDto body, HttpServletRequest request) {
        try {
            Long userId = authTokenUtils.getUserIdFromRequest(request);
            Long id = body.getId();
            RiderPayoutPolicyDto updated = policyService.updatePolicy(id, body, userId);
            return baseUtils.generateSuccessResponse(updated, UPDATE_MESSAGE, UPDATE_MESSAGE_BN);
        } catch (Exception e) {
            return baseUtils.generateErrorResponse(e);
        }
    }

    @PostMapping("/{id}/set-default")
    public BaseResponse setDefaultPolicy(@PathVariable Long id, HttpServletRequest request) {
        try {
            Long userId = authTokenUtils.getUserIdFromRequest(request);
            RiderPayoutPolicyDto updated = policyService.setDefaultPolicy(id, userId);
            return baseUtils.generateSuccessResponse(updated, "Default payout policy activated", "ডিফল্ট পেআউট পলিসি সক্রিয় হয়েছে");
        } catch (Exception e) {
            return baseUtils.generateErrorResponse(e);
        }
    }

    @DeleteMapping("/{id}")
    public BaseResponse deletePolicyWithId(@PathVariable Long id) {
        try {
            policyService.deletePolicy(id);
            return baseUtils.generateSuccessResponse(null, DELETE_MESSAGE, DELETE_MESSAGE_BN);
        } catch (Exception e) {
            return baseUtils.generateErrorResponse(e);
        }
    }

    @DeleteMapping
    public BaseResponse deletePolicyFromBody(@RequestBody(required = false) RiderPayoutPolicyDto body) {
        try {
            if (body != null && body.getId() != null) {
                policyService.deletePolicy(body.getId());
            }
            return baseUtils.generateSuccessResponse(null, DELETE_MESSAGE, DELETE_MESSAGE_BN);
        } catch (Exception e) {
            return baseUtils.generateErrorResponse(e);
        }
    }
}

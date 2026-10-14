package com.company.efood.sys.controller;

import com.company.efood.base.BasePageableRequest;
import com.company.efood.base.BaseResponse;
import com.company.efood.base.BaseUtils;
import com.company.efood.sys.dto.PayoutAccountDto;
import com.company.efood.sys.dto.PayoutDisbursementDto;
import com.company.efood.sys.services.PayoutService;
import com.company.efood.sys.utils.AuthTokenUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import static com.company.efood.base.BaseConstants.*;

@RestController
@RequiredArgsConstructor
public class PayoutController {

    private final PayoutService payoutService;
    private final BaseUtils baseUtils;
    private final AuthTokenUtils authTokenUtils;

    // Seller Payout Profile
    @GetMapping({SELLER_END_POINT + "payout-account", "/api/private/seller/payout-account"})
    public BaseResponse getSellerPayoutAccount(HttpServletRequest request) {
        try {
            Long userId = authTokenUtils.getUserIdFromRequest(request);
            PayoutAccountDto dto = payoutService.getPayoutAccount(userId, "SELLER");
            return baseUtils.generateSuccessResponse(dto, PROCESS_COMPLETE, PROCESS_COMPLETE_BN);
        } catch (Exception e) {
            return baseUtils.generateErrorResponse(e);
        }
    }

    @PostMapping({SELLER_END_POINT + "payout-account", "/api/private/seller/payout-account"})
    public BaseResponse saveSellerPayoutAccount(@RequestBody PayoutAccountDto body, HttpServletRequest request) {
        try {
            Long userId = authTokenUtils.getUserIdFromRequest(request);
            PayoutAccountDto saved = payoutService.savePayoutAccount(userId, "SELLER", body);
            return baseUtils.generateSuccessResponse(saved, "Payout profile saved successfully", "পেআউট প্রোফাইল সফলভাবে সংরক্ষণ করা হয়েছে");
        } catch (Exception e) {
            return baseUtils.generateErrorResponse(e);
        }
    }

    // Rider Payout Profile
    @GetMapping({RAIDER_END_POINT + "payout-account", "/api/private/raider/payout-account"})
    public BaseResponse getRiderPayoutAccount(HttpServletRequest request) {
        try {
            Long userId = authTokenUtils.getUserIdFromRequest(request);
            PayoutAccountDto dto = payoutService.getPayoutAccount(userId, "RAIDER");
            return baseUtils.generateSuccessResponse(dto, PROCESS_COMPLETE, PROCESS_COMPLETE_BN);
        } catch (Exception e) {
            return baseUtils.generateErrorResponse(e);
        }
    }

    @PostMapping({RAIDER_END_POINT + "payout-account", "/api/private/raider/payout-account"})
    public BaseResponse saveRiderPayoutAccount(@RequestBody PayoutAccountDto body, HttpServletRequest request) {
        try {
            Long userId = authTokenUtils.getUserIdFromRequest(request);
            PayoutAccountDto saved = payoutService.savePayoutAccount(userId, "RAIDER", body);
            return baseUtils.generateSuccessResponse(saved, "Payout profile saved successfully", "পেআউট প্রোফাইল সফলভাবে সংরক্ষণ করা হয়েছে");
        } catch (Exception e) {
            return baseUtils.generateErrorResponse(e);
        }
    }

    // Admin Weekly Settlements Panel
    @GetMapping({
            SYSTEM_ADMIN_END_POINT + "payout/weekly-settlements",
            SYSTEM_ADMIN_END_POINT + "payouts/weekly-settlements",
            "/api/private/sya/payout/weekly-settlements",
            "/api/private/sya/payouts/weekly-settlements"
    })
    public BaseResponse getWeeklySettlements(
            @RequestParam(value = "weekLabel", required = false) String weekLabel,
            @RequestParam(value = "weekOffset", required = false) Integer weekOffset) {
        try {
            List<Map<String, Object>> settlements = payoutService.getWeeklySettlements(weekLabel, weekOffset);
            return baseUtils.generateSuccessResponse(settlements, PROCESS_COMPLETE, PROCESS_COMPLETE_BN);
        } catch (Exception e) {
            return baseUtils.generateErrorResponse(e);
        }
    }

    @PostMapping({
            SYSTEM_ADMIN_END_POINT + "payout/disburse",
            SYSTEM_ADMIN_END_POINT + "payouts/disburse",
            "/api/private/sya/payout/disburse",
            "/api/private/sya/payouts/disburse"
    })
    public BaseResponse executeDisbursement(@RequestBody PayoutDisbursementDto body, HttpServletRequest request) {
        try {
            Long adminUserId = authTokenUtils.getUserIdFromRequest(request);
            PayoutDisbursementDto result = payoutService.executeDisbursement(adminUserId, body);
            return baseUtils.generateSuccessResponse(result, "Disbursement processed successfully", "অর্থ বিতরণ সফলভাবে সম্পন্ন হয়েছে");
        } catch (Exception e) {
            return baseUtils.generateErrorResponse(e);
        }
    }

    @PostMapping({
            SYSTEM_ADMIN_END_POINT + "payout/history",
            SYSTEM_ADMIN_END_POINT + "payouts/history",
            "/api/private/sya/payout/history",
            "/api/private/sya/payouts/history"
    })
    public BaseResponse getDisbursementHistory(@RequestBody BasePageableRequest request) {
        try {
            Page<PayoutDisbursementDto> page = payoutService.getDisbursementHistory(request);
            return baseUtils.generateSuccessResponse(page, PROCESS_COMPLETE, PROCESS_COMPLETE_BN);
        } catch (Exception e) {
            return baseUtils.generateErrorResponse(e);
        }
    }

    @GetMapping({
            SYSTEM_ADMIN_END_POINT + "payout/account",
            "/api/private/sya/payout/account"
    })
    public BaseResponse getAdminPayoutAccount(HttpServletRequest request) {
        try {
            Long userId = authTokenUtils.getUserIdFromRequest(request);
            PayoutAccountDto dto = payoutService.getPayoutAccount(userId, "ADMIN");
            return baseUtils.generateSuccessResponse(dto, PROCESS_COMPLETE, PROCESS_COMPLETE_BN);
        } catch (Exception e) {
            return baseUtils.generateErrorResponse(e);
        }
    }

    @PostMapping({
            SYSTEM_ADMIN_END_POINT + "payout/account",
            "/api/private/sya/payout/account"
    })
    public BaseResponse saveAdminPayoutAccount(@RequestBody PayoutAccountDto body, HttpServletRequest request) {
        try {
            Long userId = authTokenUtils.getUserIdFromRequest(request);
            PayoutAccountDto saved = payoutService.savePayoutAccount(userId, "ADMIN", body);
            return baseUtils.generateSuccessResponse(saved, "Payout profile saved successfully", "পেআউট প্রোফাইল সফলভাবে সংরক্ষণ করা হয়েছে");
        } catch (Exception e) {
            return baseUtils.generateErrorResponse(e);
        }
    }
}

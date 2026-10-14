package com.company.efood.sys.services;

import com.company.efood.base.BasePageableRequest;
import com.company.efood.sys.dto.PayoutAccountDto;
import com.company.efood.sys.dto.PayoutDisbursementDto;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Map;

public interface PayoutService {
    PayoutAccountDto getPayoutAccount(Long userId, String userType);
    PayoutAccountDto savePayoutAccount(Long userId, String userType, PayoutAccountDto dto);
    List<Map<String, Object>> getWeeklySettlements(String weekLabel);
    List<Map<String, Object>> getWeeklySettlements(String weekLabel, Integer weekOffset);
    PayoutDisbursementDto executeDisbursement(Long adminUserId, PayoutDisbursementDto dto);
    Page<PayoutDisbursementDto> getDisbursementHistory(BasePageableRequest request);
}

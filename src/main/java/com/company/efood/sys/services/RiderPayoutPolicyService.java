package com.company.efood.sys.services;

import com.company.efood.sys.dto.RiderPayoutPolicyDto;
import com.company.efood.sys.entity.RiderPayoutPolicy;

import java.math.BigDecimal;
import java.util.List;

public interface RiderPayoutPolicyService {
    List<RiderPayoutPolicyDto> getAllPolicies();
    RiderPayoutPolicyDto getActivePolicy();
    RiderPayoutPolicyDto getPolicyById(Long id);
    RiderPayoutPolicyDto createPolicy(RiderPayoutPolicyDto dto, Long userId);
    RiderPayoutPolicyDto updatePolicy(Long id, RiderPayoutPolicyDto dto, Long userId);
    RiderPayoutPolicyDto setDefaultPolicy(Long id, Long userId);
    void deletePolicy(Long id);
    org.springframework.data.domain.Page<RiderPayoutPolicyDto> getPageableAllData(com.company.efood.base.BasePageableRequest request);

    /**
     * High precision dynamic calculation of rider's share of delivery fee.
     * Guaranteed zero rounding discrepancy: total delivery fee = riderShare + adminShare.
     */
    BigDecimal calculateRiderShare(BigDecimal deliveryFee, Double distanceKm);
}

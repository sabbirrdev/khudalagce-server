package com.company.efood.sys.services.serviceimpl;

import com.company.efood.base.BasePageableRequest;
import com.company.efood.base.BaseUtils;
import com.company.efood.sys.dto.RiderPayoutPolicyDto;
import com.company.efood.sys.entity.RiderPayoutPolicy;
import com.company.efood.sys.repository.RiderPayoutPolicyRepo;
import com.company.efood.sys.services.RiderPayoutPolicyService;
import com.company.efood.sys.utils.RiderPolicyType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RiderPayoutPolicyServiceImpl implements RiderPayoutPolicyService {

    private final RiderPayoutPolicyRepo policyRepo;
    private final BaseUtils baseUtils;

    @Override
    @Transactional(readOnly = true)
    public List<RiderPayoutPolicyDto> getAllPolicies() {
        return policyRepo.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public RiderPayoutPolicyDto getActivePolicy() {
        RiderPayoutPolicy policy = policyRepo.findFirstByIsDefaultTrueAndActiveTrue()
                .orElseGet(() -> policyRepo.findAll().stream()
                        .filter(p -> Boolean.TRUE.equals(p.getActive()))
                        .findFirst()
                        .orElseGet(this::createFallbackDefaultPolicy));
        return toDto(policy);
    }

    @Override
    @Transactional(readOnly = true)
    public RiderPayoutPolicyDto getPolicyById(Long id) {
        RiderPayoutPolicy policy = policyRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Rider payout policy not found: " + id));
        return toDto(policy);
    }

    @Override
    @Transactional
    public RiderPayoutPolicyDto createPolicy(RiderPayoutPolicyDto dto, Long userId) {
        String policyName = dto.getName() != null && !dto.getName().isBlank()
                ? dto.getName().trim()
                : (dto.getPolicyName() != null ? dto.getPolicyName().trim() : null);
        if (policyName == null || policyName.isBlank()) {
            throw new IllegalArgumentException("Policy name is required");
        }

        RiderPayoutPolicy entity = new RiderPayoutPolicy();
        entity.setName(policyName);
        entity.setPolicyType(dto.getPolicyType() != null ? dto.getPolicyType() : RiderPolicyType.PERCENTAGE_SPLIT);

        if (entity.getPolicyType() == RiderPolicyType.PERCENTAGE_SPLIT) {
            BigDecimal riderPct = dto.getRiderPercentage() != null ? dto.getRiderPercentage() : new BigDecimal("80.0000");
            BigDecimal adminPct = BigDecimal.valueOf(100).subtract(riderPct);
            entity.setRiderPercentage(riderPct.setScale(4, RoundingMode.HALF_UP));
            entity.setAdminPercentage(adminPct.setScale(4, RoundingMode.HALF_UP));
        } else {
            entity.setBasePayout(dto.getBasePayout() != null ? dto.getBasePayout().setScale(4, RoundingMode.HALF_UP) : new BigDecimal("40.0000"));
            entity.setPerKmRate(dto.getPerKmRate() != null ? dto.getPerKmRate().setScale(4, RoundingMode.HALF_UP) : new BigDecimal("10.0000"));
            entity.setTierConfig(dto.getTierConfig());
        }

        entity.setDescription(dto.getDescription());
        entity.setActive(true);
        entity.setEntryUser(userId != null ? userId : 0L);
        entity.setEntryDate(LocalDateTime.now());

        if (Boolean.TRUE.equals(dto.getIsDefault())) {
            unsetOtherDefaults();
            entity.setIsDefault(true);
        } else {
            entity.setIsDefault(false);
        }

        RiderPayoutPolicy saved = policyRepo.save(entity);
        return toDto(saved);
    }

    @Override
    @Transactional
    public RiderPayoutPolicyDto updatePolicy(Long id, RiderPayoutPolicyDto dto, Long userId) {
        RiderPayoutPolicy entity = policyRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Rider payout policy not found: " + id));

        String policyName = dto.getName() != null && !dto.getName().isBlank()
                ? dto.getName().trim()
                : (dto.getPolicyName() != null ? dto.getPolicyName().trim() : null);
        if (policyName != null && !policyName.isBlank()) {
            entity.setName(policyName);
        }
        if (dto.getPolicyType() != null) {
            entity.setPolicyType(dto.getPolicyType());
        }
        if (dto.getRiderPercentage() != null) {
            BigDecimal riderPct = dto.getRiderPercentage();
            BigDecimal adminPct = BigDecimal.valueOf(100).subtract(riderPct);
            entity.setRiderPercentage(riderPct.setScale(4, RoundingMode.HALF_UP));
            entity.setAdminPercentage(adminPct.setScale(4, RoundingMode.HALF_UP));
        }
        if (dto.getBasePayout() != null) {
            entity.setBasePayout(dto.getBasePayout().setScale(4, RoundingMode.HALF_UP));
        }
        if (dto.getPerKmRate() != null) {
            entity.setPerKmRate(dto.getPerKmRate().setScale(4, RoundingMode.HALF_UP));
        }
        if (dto.getTierConfig() != null) {
            entity.setTierConfig(dto.getTierConfig());
        }
        if (dto.getDescription() != null) {
            entity.setDescription(dto.getDescription());
        }
        if (dto.getActive() != null) {
            entity.setActive(dto.getActive());
        }
        if (Boolean.TRUE.equals(dto.getIsDefault())) {
            unsetOtherDefaults();
            entity.setIsDefault(true);
        }

        entity.setUpdateUser(userId != null ? userId : 0L);
        entity.setUpdateDate(LocalDateTime.now());

        RiderPayoutPolicy saved = policyRepo.save(entity);
        return toDto(saved);
    }

    @Override
    @Transactional
    public RiderPayoutPolicyDto setDefaultPolicy(Long id, Long userId) {
        RiderPayoutPolicy entity = policyRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Rider payout policy not found: " + id));
        unsetOtherDefaults();
        entity.setIsDefault(true);
        entity.setActive(true);
        entity.setUpdateUser(userId != null ? userId : 0L);
        entity.setUpdateDate(LocalDateTime.now());
        return toDto(policyRepo.save(entity));
    }

    @Override
    @Transactional
    public void deletePolicy(Long id) {
        RiderPayoutPolicy entity = policyRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Rider payout policy not found: " + id));
        entity.setActive(false);
        policyRepo.save(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RiderPayoutPolicyDto> getPageableAllData(BasePageableRequest request) {
        PageRequest pageRequest = baseUtils.getPageRequest(request.getPage(), request.getSize());
        Page<RiderPayoutPolicy> page = policyRepo.findAll(pageRequest);
        List<RiderPayoutPolicyDto> dtos = page.stream().map(this::toDto).collect(Collectors.toList());
        return new PageImpl<>(dtos, pageRequest, page.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal calculateRiderShare(BigDecimal deliveryFee, Double distanceKm) {
        if (deliveryFee == null || deliveryFee.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        RiderPayoutPolicy policy = policyRepo.findFirstByIsDefaultTrueAndActiveTrue()
                .orElseGet(() -> policyRepo.findAll().stream()
                        .filter(p -> Boolean.TRUE.equals(p.getActive()))
                        .findFirst()
                        .orElse(null));

        if (policy == null || policy.getPolicyType() == RiderPolicyType.PERCENTAGE_SPLIT) {
            BigDecimal riderPercentage = (policy != null && policy.getRiderPercentage() != null)
                    ? policy.getRiderPercentage()
                    : new BigDecimal("80.0000");

            // Calculated as deliveryFee * (riderPercentage / 100)
            return deliveryFee.multiply(riderPercentage)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        } else {
            // TIERED policy: basePayout + (distance * perKmRate), capped at delivery fee
            BigDecimal base = policy.getBasePayout() != null ? policy.getBasePayout() : new BigDecimal("40.0000");
            BigDecimal perKm = policy.getPerKmRate() != null ? policy.getPerKmRate() : new BigDecimal("10.0000");
            double dist = distanceKm != null && distanceKm > 0 ? distanceKm : 1.0;

            BigDecimal calculated = base.add(perKm.multiply(BigDecimal.valueOf(dist))).setScale(2, RoundingMode.HALF_UP);
            // Ensure rider share does not exceed delivery fee unless deliveryFee < base
            if (deliveryFee.compareTo(base) > 0 && calculated.compareTo(deliveryFee) > 0) {
                return deliveryFee;
            }
            return calculated;
        }
    }

    private void unsetOtherDefaults() {
        List<RiderPayoutPolicy> defaults = policyRepo.findAll().stream()
                .filter(p -> Boolean.TRUE.equals(p.getIsDefault()))
                .collect(Collectors.toList());
        for (RiderPayoutPolicy p : defaults) {
            p.setIsDefault(false);
            policyRepo.save(p);
        }
    }

    private RiderPayoutPolicy createFallbackDefaultPolicy() {
        RiderPayoutPolicy fallback = new RiderPayoutPolicy();
        fallback.setName("Standard 80/20 Rider Payout Policy");
        fallback.setPolicyType(RiderPolicyType.PERCENTAGE_SPLIT);
        fallback.setRiderPercentage(new BigDecimal("80.0000"));
        fallback.setAdminPercentage(new BigDecimal("20.0000"));
        fallback.setIsDefault(true);
        fallback.setActive(true);
        fallback.setEntryUser(0L);
        fallback.setEntryDate(LocalDateTime.now());
        return policyRepo.save(fallback);
    }

    private RiderPayoutPolicyDto toDto(RiderPayoutPolicy entity) {
        RiderPayoutPolicyDto dto = new RiderPayoutPolicyDto();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setPolicyName(entity.getName());
        dto.setPolicyType(entity.getPolicyType());
        dto.setRiderPercentage(entity.getRiderPercentage());
        dto.setAdminPercentage(entity.getAdminPercentage());
        dto.setBasePayout(entity.getBasePayout());
        dto.setPerKmRate(entity.getPerKmRate());
        dto.setTierConfig(entity.getTierConfig());
        dto.setIsDefault(entity.getIsDefault());
        dto.setDescription(entity.getDescription());
        dto.setActive(entity.getActive());
        dto.setEntryDate(entity.getEntryDate());
        return dto;
    }
}

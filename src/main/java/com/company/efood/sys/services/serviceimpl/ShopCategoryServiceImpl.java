package com.company.efood.sys.services.serviceimpl;

import com.company.efood.base.BaseDropdownModel;
import com.company.efood.base.BasePageableRequest;
import com.company.efood.base.BaseUtils;
import com.company.efood.sys.dto.ShopCategoryDto;
import com.company.efood.sys.entity.AppService;
import com.company.efood.sys.entity.CommissionPolicy;
import com.company.efood.sys.entity.ShopCategory;
import com.company.efood.sys.repository.AppServiceRepo;
import com.company.efood.sys.repository.CommissionPolicyRepo;
import com.company.efood.sys.repository.ShopCategoryRepo;
import com.company.efood.sys.services.ShopCategoryService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShopCategoryServiceImpl implements ShopCategoryService {

    private final ShopCategoryRepo shopCategoryRepo;
    private final AppServiceRepo appServiceRepo;
    private final CommissionPolicyRepo commissionPolicyRepo;
    private final BaseUtils baseUtils;

    @Override
    @Transactional
    public ShopCategoryDto save(ShopCategoryDto dto, Long userId) {
        ShopCategory entity = generateEntity(dto, userId, true);
        if (entity.getCode() == null || entity.getCode().isBlank()) {
            entity.setCode(generateCodeFromName(entity.getName()));
        } else {
            entity.setCode(entity.getCode().trim().toUpperCase().replaceAll("\\s+", "_"));
        }
        ShopCategory saved = shopCategoryRepo.save(entity);
        return generateDto(saved);
    }

    @Override
    @Transactional
    public ShopCategoryDto update(ShopCategoryDto dto, Long userId) {
        ShopCategory entity = generateEntity(dto, userId, false);
        if (entity.getCode() != null) {
            entity.setCode(entity.getCode().trim().toUpperCase().replaceAll("\\s+", "_"));
        }
        ShopCategory saved = shopCategoryRepo.save(entity);
        return generateDto(saved);
    }

    @Override
    @Transactional
    public boolean delete(ShopCategoryDto dto, Long userId) {
        if (!ObjectUtils.isEmpty(dto.getId())) {
            Optional<ShopCategory> existing = shopCategoryRepo.findById(dto.getId());
            if (existing.isPresent()) {
                ShopCategory entity = existing.get();
                entity.setActive(false);
                entity.setUpdateUser(userId);
                entity.setUpdateDate(LocalDateTime.now());
                shopCategoryRepo.save(entity);
                return true;
            }
        }
        return false;
    }

    @Override
    @Transactional
    public ShopCategoryDto getById(Long id, Long userId) {
        return shopCategoryRepo.findById(id).map(this::generateDto).orElse(null);
    }

    @Override
    public List<BaseDropdownModel> getDropdownList(Long userId) {
        return null;
    }

    @Override
    @Transactional
    public Page<ShopCategoryDto> getPageableAllData(BasePageableRequest pageableBodyRequest, Long userId) {
        PageRequest pageRequest = baseUtils.getPageRequest(pageableBodyRequest.getPage(), pageableBodyRequest.getSize());
        Page<ShopCategory> page = shopCategoryRepo.findAll(pageRequest);
        List<ShopCategoryDto> list = page.stream().map(this::generateDto).collect(Collectors.toList());
        return new PageImpl<>(list, pageRequest, page.getTotalElements());
    }

    @Override
    @Transactional
    public List<ShopCategoryDto> getCategoriesByService(Long serviceId) {
        return shopCategoryRepo.findByServiceIdAndActiveTrue(serviceId).stream()
                .map(this::generateDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public List<ShopCategoryDto> getAllActiveCategories() {
        return shopCategoryRepo.findByActiveTrue().stream()
                .map(this::generateDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ShopCategoryDto getByCode(String code) {
        return shopCategoryRepo.findByCode(code).map(this::generateDto).orElse(null);
    }

    // =========================================================================
    // Helpers & Mappers
    // =========================================================================

    private ShopCategory generateEntity(ShopCategoryDto dto, Long userId, boolean isSaved) {
        ShopCategory entity = new ShopCategory();
        BeanUtils.copyProperties(dto, entity);

        // Map Service relation
        if (dto.getServiceId() != null) {
            AppService service = appServiceRepo.findById(dto.getServiceId())
                    .orElseThrow(() -> new IllegalArgumentException("Service not found with ID: " + dto.getServiceId()));
            entity.setService(service);
        }

        // Map CommissionPolicy relation (Many-to-One: multiple categories can share one policy)
        if (dto.getCommissionPolicyId() != null) {
            CommissionPolicy policy = commissionPolicyRepo.findById(dto.getCommissionPolicyId())
                    .orElseThrow(() -> new IllegalArgumentException("Commission policy not found with ID: " + dto.getCommissionPolicyId()));
            entity.setCommissionPolicy(policy);
        } else {
            entity.setCommissionPolicy(null);
        }

        if (isSaved) {
            entity.setEntryUser(userId != null ? userId : 0L);
            entity.setEntryDate(LocalDateTime.now());
            if (entity.getActive() == null) entity.setActive(true);
            if (entity.getIsApproved() == null) entity.setIsApproved(true);
        } else {
            ShopCategory dbEntity = shopCategoryRepo.findById(dto.getId()).orElse(null);
            entity.setUpdateUser(userId != null ? userId : 0L);
            entity.setUpdateDate(LocalDateTime.now());
            if (dbEntity != null) {
                baseUtils.setUpdateUserInfo(entity, dbEntity);
            }
        }
        return entity;
    }

    public ShopCategoryDto generateDto(ShopCategory entity) {
        ShopCategoryDto dto = new ShopCategoryDto();
        BeanUtils.copyProperties(entity, dto);

        if (entity.getService() != null) {
            dto.setServiceId(entity.getService().getId());
            dto.setServiceName(entity.getService().getName());
            dto.setServiceCode(entity.getService().getCode());
        }

        if (entity.getCommissionPolicy() != null) {
            dto.setCommissionPolicyId(entity.getCommissionPolicy().getId());
            dto.setCommissionPolicyName(entity.getCommissionPolicy().getName());
            dto.setCommissionPercentage(entity.getCommissionPolicy().getPercentage());
            dto.setCommissionFixedAmount(entity.getCommissionPolicy().getFixedAmount());
        }

        return dto;
    }

    private String generateCodeFromName(String name) {
        if (name == null) return "CATEGORY_" + System.currentTimeMillis();
        return name.trim().toUpperCase().replaceAll("[^A-Z0-9]+", "_");
    }
}

package com.company.efood.sys.services.serviceimpl;

import com.company.efood.base.BaseDropdownModel;
import com.company.efood.base.BasePageableRequest;
import com.company.efood.base.BaseUtils;
import com.company.efood.sys.dto.AppServiceDto;
import com.company.efood.sys.dto.ShopCategoryDto;
import com.company.efood.sys.entity.AppService;
import com.company.efood.sys.entity.ShopCategory;
import com.company.efood.sys.repository.AppServiceRepo;
import com.company.efood.sys.repository.ShopCategoryRepo;
import com.company.efood.sys.services.AppServiceService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AppServiceServiceImpl implements AppServiceService {

    private final AppServiceRepo appServiceRepo;
    private final ShopCategoryRepo shopCategoryRepo;
    private final BaseUtils baseUtils;
    private final ModelMapper modelMapper;

    @Override
    @Transactional
    public AppServiceDto save(AppServiceDto dto, Long userId) {
        AppService entity = generateEntity(dto, userId, true);
        if (entity.getCode() == null || entity.getCode().isBlank()) {
            entity.setCode(generateCodeFromName(entity.getName()));
        } else {
            entity.setCode(entity.getCode().trim().toUpperCase().replaceAll("\\s+", "_"));
        }
        AppService saved = appServiceRepo.save(entity);
        return generateDto(saved);
    }

    @Override
    @Transactional
    public AppServiceDto update(AppServiceDto dto, Long userId) {
        AppService entity = generateEntity(dto, userId, false);
        if (entity.getCode() != null) {
            entity.setCode(entity.getCode().trim().toUpperCase().replaceAll("\\s+", "_"));
        }
        AppService saved = appServiceRepo.save(entity);
        return generateDto(saved);
    }

    @Override
    @Transactional
    public boolean delete(AppServiceDto dto, Long userId) {
        if (!ObjectUtils.isEmpty(dto.getId())) {
            Optional<AppService> existing = appServiceRepo.findById(dto.getId());
            if (existing.isPresent()) {
                AppService entity = existing.get();
                entity.setActive(false);
                entity.setUpdateUser(userId);
                entity.setUpdateDate(LocalDateTime.now());
                appServiceRepo.save(entity);
                return true;
            }
        }
        return false;
    }

    @Override
    @Transactional
    public AppServiceDto getById(Long id, Long userId) {
        return appServiceRepo.findById(id).map(this::generateDtoWithCategories).orElse(null);
    }

    @Override
    public List<BaseDropdownModel> getDropdownList(Long userId) {
        return null;
    }

    @Override
    @Transactional
    public Page<AppServiceDto> getPageableAllData(BasePageableRequest pageableBodyRequest, Long userId) {
        PageRequest pageRequest = baseUtils.getPageRequest(pageableBodyRequest.getPage(), pageableBodyRequest.getSize());
        Page<AppService> page = appServiceRepo.findAllByOrderByDisplayOrderAsc(pageRequest);
        List<AppServiceDto> list = page.stream().map(this::generateDtoWithCategories).collect(Collectors.toList());
        return new PageImpl<>(list, pageRequest, page.getTotalElements());
    }

    @Override
    @Transactional
    public List<AppServiceDto> getActiveServices() {
        return appServiceRepo.findByActiveTrueOrderByDisplayOrderAsc().stream()
                .map(this::generateDtoWithCategories)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public AppServiceDto getByCode(String code) {
        return appServiceRepo.findByCode(code).map(this::generateDtoWithCategories).orElse(null);
    }

    // =========================================================================
    // Helpers & Mappers
    // =========================================================================

    private AppService generateEntity(AppServiceDto dto, Long userId, boolean isSaved) {
        AppService entity = new AppService();
        BeanUtils.copyProperties(dto, entity);
        if (isSaved) {
            entity.setEntryUser(userId != null ? userId : 0L);
            entity.setEntryDate(LocalDateTime.now());
            if (entity.getActive() == null) entity.setActive(true);
            if (entity.getIsApproved() == null) entity.setIsApproved(true);
        } else {
            AppService dbEntity = appServiceRepo.findById(dto.getId()).orElse(null);
            entity.setUpdateUser(userId != null ? userId : 0L);
            entity.setUpdateDate(LocalDateTime.now());
            if (dbEntity != null) {
                baseUtils.setUpdateUserInfo(entity, dbEntity);
            }
        }
        return entity;
    }

    private AppServiceDto generateDto(AppService entity) {
        AppServiceDto dto = modelMapper.map(entity, AppServiceDto.class);
        return dto;
    }

    private AppServiceDto generateDtoWithCategories(AppService entity) {
        AppServiceDto dto = generateDto(entity);
        List<ShopCategory> categories = shopCategoryRepo.findByServiceIdAndActiveTrue(entity.getId());
        List<ShopCategoryDto> catDtos = new ArrayList<>();
        if (categories != null) {
            for (ShopCategory cat : categories) {
                ShopCategoryDto cd = new ShopCategoryDto();
                BeanUtils.copyProperties(cat, cd);
                cd.setServiceId(entity.getId());
                cd.setServiceName(entity.getName());
                cd.setServiceCode(entity.getCode());
                if (cat.getCommissionPolicy() != null) {
                    cd.setCommissionPolicyId(cat.getCommissionPolicy().getId());
                    cd.setCommissionPolicyName(cat.getCommissionPolicy().getName());
                    cd.setCommissionPercentage(cat.getCommissionPolicy().getPercentage());
                    cd.setCommissionFixedAmount(cat.getCommissionPolicy().getFixedAmount());
                }
                catDtos.add(cd);
            }
        }
        dto.setShopCategories(catDtos);
        dto.setCategoryCount(catDtos.size());
        return dto;
    }

    private String generateCodeFromName(String name) {
        if (name == null) return "SERVICE_" + System.currentTimeMillis();
        return name.trim().toUpperCase().replaceAll("[^A-Z0-9]+", "_");
    }
}

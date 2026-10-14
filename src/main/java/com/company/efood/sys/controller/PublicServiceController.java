package com.company.efood.sys.controller;

import com.company.efood.base.BasePageableRequest;
import com.company.efood.base.BaseResponse;
import com.company.efood.base.BaseUtils;
import com.company.efood.sys.dto.AppServiceDto;
import com.company.efood.sys.dto.ShopCategoryDto;
import com.company.efood.sys.entity.Shop;
import com.company.efood.sys.repository.ShopRepo;
import com.company.efood.sys.services.AppServiceService;
import com.company.efood.sys.services.ShopCategoryService;
import com.company.efood.sys.services.ShopService;
import com.company.efood.sys.dto.ShopDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.company.efood.base.BaseConstants.PROCESS_COMPLETE;
import static com.company.efood.base.BaseConstants.PROCESS_COMPLETE_BN;
import static com.company.efood.base.BaseConstants.PUBLIC_ENDPOINT;

@Slf4j
@RestController
@RequestMapping(PUBLIC_ENDPOINT)
@RequiredArgsConstructor
public class PublicServiceController {

    private final AppServiceService appServiceService;
    private final ShopCategoryService shopCategoryService;
    private final ShopService shopService;
    private final BaseUtils baseUtils;

    /**
     * GET /api/public/services
     * Returns all active services/verticals with their underlying shop categories.
     */
    @GetMapping("services")
    public BaseResponse getActiveServices() {
        try {
            List<AppServiceDto> services = appServiceService.getActiveServices();
            return baseUtils.generateSuccessResponse(services, PROCESS_COMPLETE, PROCESS_COMPLETE_BN);
        } catch (Exception e) {
            log.error("Error fetching active services", e);
            return baseUtils.generateErrorResponse(e);
        }
    }

    /**
     * GET /api/public/services/{serviceId}
     * Returns a specific service by ID.
     */
    @GetMapping("services/{serviceId:[0-9]+}")
    public BaseResponse getServiceById(@PathVariable Long serviceId) {
        try {
            AppServiceDto dto = appServiceService.getById(serviceId, 0L);
            return baseUtils.generateSuccessResponse(dto, PROCESS_COMPLETE, PROCESS_COMPLETE_BN);
        } catch (Exception e) {
            return baseUtils.generateErrorResponse(e);
        }
    }

    /**
     * GET /api/public/services/{serviceId}/vendors
     * Returns all active vendors/shops mapped to the given Service ID.
     */
    @GetMapping("services/{serviceId:[0-9]+}/vendors")
    public BaseResponse getVendorsByServiceId(@PathVariable Long serviceId) {
        try {
            List<ShopDto> shops = shopService.getShopsByServiceId(serviceId);
            return baseUtils.generateSuccessResponse(shops, PROCESS_COMPLETE, PROCESS_COMPLETE_BN);
        } catch (Exception e) {
            log.error("Error fetching vendors for serviceId {}", serviceId, e);
            return baseUtils.generateErrorResponse(e);
        }
    }

    /**
     * POST /api/public/services/{serviceId}/vendors
     * Paginated retrieval of active vendors for a specific Service ID with search support.
     */
    @PostMapping("services/{serviceId:[0-9]+}/vendors")
    public BaseResponse getVendorsByServiceIdPageable(
            @PathVariable Long serviceId,
            @Valid @RequestBody BasePageableRequest pageableRequest) {
        try {
            Page<ShopDto> page = shopService.getShopsByServiceIdPageable(serviceId, pageableRequest);
            return baseUtils.generateSuccessResponse(page, PROCESS_COMPLETE, PROCESS_COMPLETE_BN);
        } catch (Exception e) {
            log.error("Error fetching pageable vendors for serviceId {}", serviceId, e);
            return baseUtils.generateErrorResponse(e);
        }
    }

    /**
     * GET /api/public/shop-categories
     * Returns all active shop categories.
     */
    @GetMapping("shop-categories")
    public BaseResponse getAllActiveShopCategories() {
        try {
            List<ShopCategoryDto> categories = shopCategoryService.getAllActiveCategories();
            return baseUtils.generateSuccessResponse(categories, PROCESS_COMPLETE, PROCESS_COMPLETE_BN);
        } catch (Exception e) {
            return baseUtils.generateErrorResponse(e);
        }
    }

    /**
     * GET /api/public/shop-categories/by-service/{serviceId}
     * Returns shop categories for a specific service (used during seller registration).
     */
    @GetMapping("shop-categories/by-service/{serviceId:[0-9]+}")
    public BaseResponse getCategoriesByService(@PathVariable Long serviceId) {
        try {
            List<ShopCategoryDto> categories = shopCategoryService.getCategoriesByService(serviceId);
            return baseUtils.generateSuccessResponse(categories, PROCESS_COMPLETE, PROCESS_COMPLETE_BN);
        } catch (Exception e) {
            return baseUtils.generateErrorResponse(e);
        }
    }
}

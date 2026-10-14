package com.company.efood.sys.controller;

import com.company.efood.base.BaseController;
import com.company.efood.base.BasePageableRequest;
import com.company.efood.base.BaseResponse;
import com.company.efood.base.BaseUtils;
import com.company.efood.sys.dto.ShopCategoryDto;
import com.company.efood.sys.services.ShopCategoryService;
import com.company.efood.sys.utils.AuthTokenUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import static com.company.efood.base.BaseConstants.*;

@RestController
@RequestMapping(SYSTEM_ADMIN_END_POINT + "shop-categories")
@RequiredArgsConstructor
public class ShopCategoryAdminController implements BaseController<ShopCategoryDto> {

    private final ShopCategoryService shopCategoryService;
    private final BaseUtils baseUtils;
    private final AuthTokenUtils authTokenUtils;

    @Override
    @PostMapping
    public BaseResponse save(@Valid @RequestBody ShopCategoryDto body, HttpServletRequest request) {
        try {
            return baseUtils.generateSuccessResponse(shopCategoryService.save(body, getUserIdSafe(request)), SAVE_MESSAGE, SAVE_MESSAGE_BN);
        } catch (Exception e) {
            return baseUtils.generateErrorResponse(e);
        }
    }

    @Override
    @PutMapping
    public BaseResponse update(@Valid @RequestBody ShopCategoryDto body, HttpServletRequest request) {
        try {
            return baseUtils.generateSuccessResponse(shopCategoryService.update(body, getUserIdSafe(request)), UPDATE_MESSAGE, UPDATE_MESSAGE_BN);
        } catch (Exception e) {
            return baseUtils.generateErrorResponse(e);
        }
    }

    @Override
    @DeleteMapping
    public BaseResponse delete(@Valid @RequestBody ShopCategoryDto body, HttpServletRequest request) {
        try {
            if (shopCategoryService.delete(body, getUserIdSafe(request))) {
                return baseUtils.generateSuccessResponse(null, DELETE_MESSAGE, DELETE_MESSAGE_BN);
            } else {
                return baseUtils.generateSuccessResponse(null, DELETE_MESSAGE_FAILED, DELETE_MESSAGE_FAILED_BN);
            }
        } catch (Exception e) {
            return baseUtils.generateErrorResponse(e);
        }
    }

    @DeleteMapping("/{id}")
    public BaseResponse deleteById(@PathVariable Long id, HttpServletRequest request) {
        try {
            ShopCategoryDto dto = new ShopCategoryDto();
            dto.setId(id);
            if (shopCategoryService.delete(dto, getUserIdSafe(request))) {
                return baseUtils.generateSuccessResponse(null, DELETE_MESSAGE, DELETE_MESSAGE_BN);
            } else {
                return baseUtils.generateSuccessResponse(null, DELETE_MESSAGE_FAILED, DELETE_MESSAGE_FAILED_BN);
            }
        } catch (Exception e) {
            return baseUtils.generateErrorResponse(e);
        }
    }

    @Override
    @GetMapping(value = GET_OBJECT_BY_ID, produces = EXTERNAL_MEDIA_TYPE)
    public BaseResponse getById(@PathVariable(OBJECT_ID) Long id, HttpServletRequest request) {
        try {
            return baseUtils.generateSuccessResponse(shopCategoryService.getById(id, getUserIdSafe(request)));
        } catch (Exception e) {
            return baseUtils.generateErrorResponse(e);
        }
    }

    @Override
    public BaseResponse getDropdownList(HttpServletRequest request) {
        try {
            return baseUtils.generateSuccessResponse(shopCategoryService.getAllActiveCategories());
        } catch (Exception e) {
            return baseUtils.generateErrorResponse(e);
        }
    }

    @GetMapping("/by-service/{serviceId}")
    public BaseResponse getByService(@PathVariable Long serviceId) {
        try {
            return baseUtils.generateSuccessResponse(shopCategoryService.getCategoriesByService(serviceId));
        } catch (Exception e) {
            return baseUtils.generateErrorResponse(e);
        }
    }

    @Override
    @PutMapping(value = PAGEABLE_DATA_PATH)
    public BaseResponse getAllPageableData(@Valid @RequestBody BasePageableRequest basePageableRequest, HttpServletRequest request) {
        try {
            return baseUtils.generateSuccessResponse(shopCategoryService.getPageableAllData(basePageableRequest, getUserIdSafe(request)));
        } catch (Exception e) {
            return baseUtils.generateErrorResponse(e);
        }
    }

    @PostMapping(value = PAGEABLE_DATA_PATH)
    public BaseResponse getAllPageableDataPost(@Valid @RequestBody BasePageableRequest basePageableRequest, HttpServletRequest request) {
        try {
            return baseUtils.generateSuccessResponse(shopCategoryService.getPageableAllData(basePageableRequest, getUserIdSafe(request)));
        } catch (Exception e) {
            return baseUtils.generateErrorResponse(e);
        }
    }

    private Long getUserIdSafe(HttpServletRequest request) {
        try {
            return authTokenUtils.getUserIdFromRequest(request);
        } catch (Exception e) {
            return 0L;
        }
    }
}

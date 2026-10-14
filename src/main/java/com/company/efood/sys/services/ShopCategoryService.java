package com.company.efood.sys.services;

import com.company.efood.base.BaseService;
import com.company.efood.sys.dto.ShopCategoryDto;

import java.util.List;

public interface ShopCategoryService extends BaseService<ShopCategoryDto> {

    List<ShopCategoryDto> getCategoriesByService(Long serviceId);

    List<ShopCategoryDto> getAllActiveCategories();

    ShopCategoryDto getByCode(String code);
}

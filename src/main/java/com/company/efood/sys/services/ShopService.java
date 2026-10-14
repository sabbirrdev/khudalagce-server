package com.company.efood.sys.services;

import com.company.efood.base.BaseService;
import com.company.efood.sys.dto.ShopDto;

public interface ShopService extends BaseService<ShopDto> {
    /** Returns the shop belonging to the currently logged-in seller, or null if none. */
    ShopDto getMyShop(Long userId);

    /** Returns active, approved shops/vendors for a given service ID. */
    java.util.List<ShopDto> getShopsByServiceId(Long serviceId);

    /** Returns paginated active, approved shops/vendors for a given service ID. */
    org.springframework.data.domain.Page<ShopDto> getShopsByServiceIdPageable(Long serviceId, com.company.efood.base.BasePageableRequest pageableRequest);
}

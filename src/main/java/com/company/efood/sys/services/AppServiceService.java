package com.company.efood.sys.services;

import com.company.efood.base.BaseService;
import com.company.efood.sys.dto.AppServiceDto;

import java.util.List;

public interface AppServiceService extends BaseService<AppServiceDto> {

    List<AppServiceDto> getActiveServices();

    AppServiceDto getByCode(String code);
}

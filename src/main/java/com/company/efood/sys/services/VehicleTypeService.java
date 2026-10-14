package com.company.efood.sys.services;

import com.company.efood.base.BasePageableRequest;
import com.company.efood.sys.dto.VehicleTypeDto;
import com.company.efood.sys.entity.VehicleType;
import org.springframework.data.domain.Page;

import java.util.List;

public interface VehicleTypeService {
    List<VehicleTypeDto> getAllVehicleTypes();
    List<VehicleTypeDto> getActiveVehicleTypes();
    VehicleTypeDto getVehicleTypeById(Long id);
    VehicleTypeDto createVehicleType(VehicleTypeDto dto, Long userId);
    VehicleTypeDto updateVehicleType(Long id, VehicleTypeDto dto, Long userId);
    void deleteVehicleType(Long id);
    Page<VehicleTypeDto> getPageableAllData(BasePageableRequest request);
}

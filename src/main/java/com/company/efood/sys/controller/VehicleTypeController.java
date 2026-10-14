package com.company.efood.sys.controller;

import com.company.efood.base.BaseResponse;
import com.company.efood.base.BaseUtils;
import com.company.efood.sys.dto.VehicleTypeDto;
import com.company.efood.sys.services.VehicleTypeService;
import com.company.efood.sys.utils.AuthTokenUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.company.efood.base.BaseConstants.*;

@RestController
@RequiredArgsConstructor
public class VehicleTypeController {

    private final VehicleTypeService vehicleTypeService;
    private final BaseUtils baseUtils;
    private final AuthTokenUtils authTokenUtils;

    // Public endpoint for rider onboarding
    @GetMapping({"/api/public/vehicle-types", "/api/public/raider/vehicle-types"})
    public BaseResponse getPublicActiveVehicleTypes() {
        try {
            List<VehicleTypeDto> list = vehicleTypeService.getActiveVehicleTypes();
            return baseUtils.generateSuccessResponse(list, PROCESS_COMPLETE, PROCESS_COMPLETE_BN);
        } catch (Exception e) {
            return baseUtils.generateErrorResponse(e);
        }
    }

    // Admin CRUD endpoints
    @GetMapping({SYSTEM_ADMIN_END_POINT + "vehicle-types", "/api/private/sya/vehicle-types"})
    public BaseResponse getAllVehicleTypes() {
        try {
            List<VehicleTypeDto> list = vehicleTypeService.getAllVehicleTypes();
            return baseUtils.generateSuccessResponse(list, PROCESS_COMPLETE, PROCESS_COMPLETE_BN);
        } catch (Exception e) {
            return baseUtils.generateErrorResponse(e);
        }
    }

    @PutMapping({SYSTEM_ADMIN_END_POINT + "vehicle-types/pageable-data", "/api/private/sya/vehicle-types/pageable-data"})
    public BaseResponse getPageableDataPut(@RequestBody com.company.efood.base.BasePageableRequest request) {
        try {
            org.springframework.data.domain.Page<VehicleTypeDto> page = vehicleTypeService.getPageableAllData(request);
            return baseUtils.generateSuccessResponse(page, PROCESS_COMPLETE, PROCESS_COMPLETE_BN);
        } catch (Exception e) {
            return baseUtils.generateErrorResponse(e);
        }
    }

    @PostMapping({SYSTEM_ADMIN_END_POINT + "vehicle-types/pageable-data", "/api/private/sya/vehicle-types/pageable-data"})
    public BaseResponse getPageableDataPost(@RequestBody com.company.efood.base.BasePageableRequest request) {
        try {
            org.springframework.data.domain.Page<VehicleTypeDto> page = vehicleTypeService.getPageableAllData(request);
            return baseUtils.generateSuccessResponse(page, PROCESS_COMPLETE, PROCESS_COMPLETE_BN);
        } catch (Exception e) {
            return baseUtils.generateErrorResponse(e);
        }
    }

    @GetMapping({SYSTEM_ADMIN_END_POINT + "vehicle-types/{id}", SYSTEM_ADMIN_END_POINT + "vehicle-types/get-by-id/{id}", "/api/private/sya/vehicle-types/{id}", "/api/private/sya/vehicle-types/get-by-id/{id}"})
    public BaseResponse getVehicleTypeById(@PathVariable Long id) {
        try {
            VehicleTypeDto dto = vehicleTypeService.getVehicleTypeById(id);
            return baseUtils.generateSuccessResponse(dto, PROCESS_COMPLETE, PROCESS_COMPLETE_BN);
        } catch (Exception e) {
            return baseUtils.generateErrorResponse(e);
        }
    }

    @PostMapping({SYSTEM_ADMIN_END_POINT + "vehicle-types", "/api/private/sya/vehicle-types"})
    public BaseResponse createVehicleType(@RequestBody VehicleTypeDto body, HttpServletRequest request) {
        try {
            Long userId = authTokenUtils.getUserIdFromRequest(request);
            VehicleTypeDto created = vehicleTypeService.createVehicleType(body, userId);
            return baseUtils.generateSuccessResponse(created, SAVE_MESSAGE, SAVE_MESSAGE_BN);
        } catch (Exception e) {
            return baseUtils.generateErrorResponse(e);
        }
    }

    @PutMapping({SYSTEM_ADMIN_END_POINT + "vehicle-types/{id}", "/api/private/sya/vehicle-types/{id}"})
    public BaseResponse updateVehicleTypeWithId(@PathVariable Long id, @RequestBody VehicleTypeDto body, HttpServletRequest request) {
        try {
            Long userId = authTokenUtils.getUserIdFromRequest(request);
            VehicleTypeDto updated = vehicleTypeService.updateVehicleType(id, body, userId);
            return baseUtils.generateSuccessResponse(updated, UPDATE_MESSAGE, UPDATE_MESSAGE_BN);
        } catch (Exception e) {
            return baseUtils.generateErrorResponse(e);
        }
    }

    @PutMapping({SYSTEM_ADMIN_END_POINT + "vehicle-types", "/api/private/sya/vehicle-types"})
    public BaseResponse updateVehicleTypeFromBody(@RequestBody VehicleTypeDto body, HttpServletRequest request) {
        try {
            Long userId = authTokenUtils.getUserIdFromRequest(request);
            Long id = body.getId();
            VehicleTypeDto updated = vehicleTypeService.updateVehicleType(id, body, userId);
            return baseUtils.generateSuccessResponse(updated, UPDATE_MESSAGE, UPDATE_MESSAGE_BN);
        } catch (Exception e) {
            return baseUtils.generateErrorResponse(e);
        }
    }

    @DeleteMapping({SYSTEM_ADMIN_END_POINT + "vehicle-types/{id}", "/api/private/sya/vehicle-types/{id}"})
    public BaseResponse deleteVehicleTypeWithId(@PathVariable Long id) {
        try {
            vehicleTypeService.deleteVehicleType(id);
            return baseUtils.generateSuccessResponse(null, DELETE_MESSAGE, DELETE_MESSAGE_BN);
        } catch (Exception e) {
            return baseUtils.generateErrorResponse(e);
        }
    }

    @DeleteMapping({SYSTEM_ADMIN_END_POINT + "vehicle-types", "/api/private/sya/vehicle-types"})
    public BaseResponse deleteVehicleTypeFromBody(@RequestBody(required = false) VehicleTypeDto body) {
        try {
            if (body != null && body.getId() != null) {
                vehicleTypeService.deleteVehicleType(body.getId());
            }
            return baseUtils.generateSuccessResponse(null, DELETE_MESSAGE, DELETE_MESSAGE_BN);
        } catch (Exception e) {
            return baseUtils.generateErrorResponse(e);
        }
    }
}

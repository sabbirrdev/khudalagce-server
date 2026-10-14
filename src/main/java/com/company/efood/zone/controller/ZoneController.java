package com.company.efood.zone.controller;

import com.company.efood.base.BaseResponse;
import com.company.efood.sys.utils.AuthTokenUtils;
import com.company.efood.zone.dto.DistrictDto;
import com.company.efood.zone.dto.DivisionDto;
import com.company.efood.zone.dto.UpazilaDto;
import com.company.efood.zone.dto.ZoneDto;
import com.company.efood.zone.model.ZoneCalculateFeeRequest;
import com.company.efood.zone.service.ZoneService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class ZoneController {

    private final ZoneService zoneService;
    private final AuthTokenUtils authTokenUtils;

    // PUBLIC ENDPOINTS
    @GetMapping("/api/public/zone/divisions")
    public BaseResponse getAllDivisions() {
        return BaseResponse.builder().status(true).statusCode(200).data(zoneService.getAllDivisions()).build();
    }

    @GetMapping("/api/public/zone/districts/{divisionId}")
    public BaseResponse getDistrictsByDivision(@PathVariable Long divisionId) {
        return BaseResponse.builder().status(true).statusCode(200).data(zoneService.getDistrictsByDivision(divisionId)).build();
    }

    @GetMapping("/api/public/zone/upazilas/{districtId}")
    public BaseResponse getUpazilasByDistrict(@PathVariable Long districtId) {
        return BaseResponse.builder().status(true).statusCode(200).data(zoneService.getUpazilasByDistrict(districtId)).build();
    }

    @GetMapping("/api/public/zone/zones/{upazilaId}")
    public BaseResponse getZonesByUpazila(@PathVariable Long upazilaId) {
        return BaseResponse.builder().status(true).statusCode(200).data(zoneService.getZonesByUpazila(upazilaId)).build();
    }

    @GetMapping("/api/public/zone/{zoneId}")
    public BaseResponse getZoneById(@PathVariable Long zoneId) {
        return BaseResponse.builder().status(true).statusCode(200).data(zoneService.getZoneById(zoneId)).build();
    }

    @PostMapping("/api/public/zone/calculate-fee")
    public BaseResponse calculateFee(@RequestBody ZoneCalculateFeeRequest request) {
        return BaseResponse.builder().status(true).statusCode(200).data(
                zoneService.calculateDeliveryFee(request.getZoneId(), request.getLat(), request.getLon())
        ).build();
    }

    // PRIVATE ENDPOINTS
    @PostMapping("/api/private/sya/zone/division")
    public BaseResponse saveDivision(@RequestBody DivisionDto dto, HttpServletRequest request) {
        Long userId = authTokenUtils.getUserIdFromRequest(request);
        return BaseResponse.builder().status(true).statusCode(200).data(zoneService.saveDivision(dto, userId)).build();
    }

    @PostMapping("/api/private/sya/zone/district")
    public BaseResponse saveDistrict(@RequestBody DistrictDto dto, HttpServletRequest request) {
        Long userId = authTokenUtils.getUserIdFromRequest(request);
        return BaseResponse.builder().status(true).statusCode(200).data(zoneService.saveDistrict(dto, userId)).build();
    }

    @PostMapping("/api/private/sya/zone/upazila")
    public BaseResponse saveUpazila(@RequestBody UpazilaDto dto, HttpServletRequest request) {
        Long userId = authTokenUtils.getUserIdFromRequest(request);
        return BaseResponse.builder().status(true).statusCode(200).data(zoneService.saveUpazila(dto, userId)).build();
    }

    @PostMapping("/api/private/sya/zone/zone")
    public BaseResponse saveZone(@RequestBody ZoneDto dto, HttpServletRequest request) {
        Long userId = authTokenUtils.getUserIdFromRequest(request);
        return BaseResponse.builder().status(true).statusCode(200).data(zoneService.saveZone(dto, userId)).build();
    }

    @PutMapping("/api/private/sya/zone/zone/{id}")
    public BaseResponse updateZone(@PathVariable Long id, @RequestBody ZoneDto dto, HttpServletRequest request) {
        Long userId = authTokenUtils.getUserIdFromRequest(request);
        return BaseResponse.builder().status(true).statusCode(200).data(zoneService.updateZone(id, dto, userId)).build();
    }

    @GetMapping("/api/private/sya/zone/divisions/all")
    public BaseResponse getAllDivisionsAdmin() {
        return BaseResponse.builder().status(true).statusCode(200).data(zoneService.getAllDivisionsAdmin()).build();
    }

    @GetMapping("/api/private/sya/zone/districts/all")
    public BaseResponse getAllDistricts() {
        return BaseResponse.builder().status(true).statusCode(200).data(zoneService.getAllDistricts()).build();
    }

    @GetMapping("/api/private/sya/zone/upazilas/all")
    public BaseResponse getAllUpazilas() {
        return BaseResponse.builder().status(true).statusCode(200).data(zoneService.getAllUpazilas()).build();
    }

    @GetMapping("/api/private/sya/zone/zones/all")
    public BaseResponse getAllZonesAdmin() {
        return BaseResponse.builder().status(true).statusCode(200).data(zoneService.getAllZonesAdmin()).build();
    }

    @DeleteMapping("/api/private/sya/zone/zone/{id}")
    public BaseResponse deleteZone(@PathVariable Long id) {
        return BaseResponse.builder().status(true).statusCode(200).data(zoneService.deleteZone(id)).message("Zone deleted successfully").build();
    }

    @DeleteMapping("/api/private/sya/zone/zones/cleanup-all")
    public BaseResponse deleteAllZones() {
        return BaseResponse.builder().status(true).statusCode(200).data(zoneService.deleteAllZones()).message("All zones removed successfully").build();
    }

    @DeleteMapping("/api/private/sya/zone/upazila/{id}")
    public BaseResponse deleteUpazila(@PathVariable Long id) {
        return BaseResponse.builder().status(true).statusCode(200).data(zoneService.deleteUpazila(id)).message("Upazila deleted successfully").build();
    }

    @DeleteMapping("/api/private/sya/zone/district/{id}")
    public BaseResponse deleteDistrict(@PathVariable Long id) {
        return BaseResponse.builder().status(true).statusCode(200).data(zoneService.deleteDistrict(id)).message("District deleted successfully").build();
    }

    @DeleteMapping("/api/private/sya/zone/division/{id}")
    public BaseResponse deleteDivision(@PathVariable Long id) {
        return BaseResponse.builder().status(true).statusCode(200).data(zoneService.deleteDivision(id)).message("Division deleted successfully").build();
    }
}

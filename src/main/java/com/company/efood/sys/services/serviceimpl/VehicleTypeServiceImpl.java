package com.company.efood.sys.services.serviceimpl;

import com.company.efood.base.BaseUtils;
import com.company.efood.sys.dto.VehicleTypeDto;
import com.company.efood.sys.entity.VehicleType;
import com.company.efood.sys.repository.VehicleTypeRepo;
import com.company.efood.sys.services.VehicleTypeService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VehicleTypeServiceImpl implements VehicleTypeService {

    private final VehicleTypeRepo vehicleTypeRepo;
    private final ModelMapper modelMapper;
    private final BaseUtils baseUtils;

    @Override
    @Transactional(readOnly = true)
    public List<VehicleTypeDto> getAllVehicleTypes() {
        return vehicleTypeRepo.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<VehicleTypeDto> getActiveVehicleTypes() {
        return vehicleTypeRepo.findByActiveTrueOrderByIdAsc().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public VehicleTypeDto getVehicleTypeById(Long id) {
        VehicleType entity = vehicleTypeRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Vehicle type not found: " + id));
        return toDto(entity);
    }

    @Override
    @Transactional
    public VehicleTypeDto createVehicleType(VehicleTypeDto dto, Long userId) {
        if (dto.getName() == null || dto.getName().isBlank()) {
            throw new IllegalArgumentException("Vehicle type name is required");
        }
        String code = dto.getCode() != null && !dto.getCode().isBlank()
                ? dto.getCode().trim().toUpperCase().replaceAll("\\s+", "_")
                : dto.getName().trim().toUpperCase().replaceAll("\\s+", "_");

        if (vehicleTypeRepo.findByCodeIgnoreCase(code).isPresent()) {
            throw new IllegalArgumentException("Vehicle type with code " + code + " already exists");
        }

        VehicleType entity = new VehicleType();
        entity.setName(dto.getName().trim());
        entity.setCode(code);
        entity.setDescription(dto.getDescription());
        entity.setRequiredDocuments(dto.getRequiredDocuments() != null ? new ArrayList<>(dto.getRequiredDocuments()) : new ArrayList<>());
        entity.setActive(true);
        entity.setEntryUser(userId != null ? userId : 0L);
        entity.setEntryDate(LocalDateTime.now());

        VehicleType saved = vehicleTypeRepo.save(entity);
        return toDto(saved);
    }

    @Override
    @Transactional
    public VehicleTypeDto updateVehicleType(Long id, VehicleTypeDto dto, Long userId) {
        VehicleType entity = vehicleTypeRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Vehicle type not found: " + id));

        if (dto.getName() != null && !dto.getName().isBlank()) {
            entity.setName(dto.getName().trim());
        }
        if (dto.getCode() != null && !dto.getCode().isBlank()) {
            String newCode = dto.getCode().trim().toUpperCase().replaceAll("\\s+", "_");
            vehicleTypeRepo.findByCodeIgnoreCase(newCode).ifPresent(existing -> {
                if (!existing.getId().equals(id)) {
                    throw new IllegalArgumentException("Vehicle type code " + newCode + " already exists");
                }
            });
            entity.setCode(newCode);
        }
        if (dto.getDescription() != null) {
            entity.setDescription(dto.getDescription());
        }
        if (dto.getRequiredDocuments() != null) {
            entity.getRequiredDocuments().clear();
            entity.getRequiredDocuments().addAll(dto.getRequiredDocuments());
        }
        if (dto.getActive() != null) {
            entity.setActive(dto.getActive());
        }
        entity.setUpdateUser(userId != null ? userId : 0L);
        entity.setUpdateDate(LocalDateTime.now());

        VehicleType saved = vehicleTypeRepo.save(entity);
        return toDto(saved);
    }

    @Override
    @Transactional
    public void deleteVehicleType(Long id) {
        VehicleType entity = vehicleTypeRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Vehicle type not found: " + id));
        entity.setActive(false);
        vehicleTypeRepo.save(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<VehicleTypeDto> getPageableAllData(com.company.efood.base.BasePageableRequest request) {
        org.springframework.data.domain.PageRequest pageRequest = baseUtils.getPageRequest(request.getPage(), request.getSize());
        org.springframework.data.domain.Page<VehicleType> page = vehicleTypeRepo.findAll(pageRequest);
        List<VehicleTypeDto> dtos = page.stream().map(this::toDto).collect(Collectors.toList());
        return new org.springframework.data.domain.PageImpl<>(dtos, pageRequest, page.getTotalElements());
    }

    private VehicleTypeDto toDto(VehicleType entity) {
        VehicleTypeDto dto = new VehicleTypeDto();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setCode(entity.getCode());
        dto.setDescription(entity.getDescription());
        dto.setRequiredDocuments(entity.getRequiredDocuments() != null ? new ArrayList<>(entity.getRequiredDocuments()) : new ArrayList<>());
        dto.setActive(entity.getActive());
        dto.setEntryDate(entity.getEntryDate());
        return dto;
    }
}

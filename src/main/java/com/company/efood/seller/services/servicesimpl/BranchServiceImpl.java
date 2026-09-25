package com.company.efood.seller.services.servicesimpl;

import com.company.efood.base.BaseDropdownModel;
import com.company.efood.base.BasePageableRequest;
import com.company.efood.base.BaseUtils;
import com.company.efood.config.CurrentUserContext;
import com.company.efood.seller.dto.BranchDto;
import com.company.efood.seller.entity.Seller;
import com.company.efood.seller.repository.BranchRepo;
import com.company.efood.seller.repository.SellerRepo;
import com.company.efood.seller.services.BranchService;
import com.company.efood.sys.dto.AddressDto;
import com.company.efood.sys.entity.Address;
import com.company.efood.sys.entity.Branch;
import com.company.efood.sys.entity.Shop;
import com.company.efood.sys.repository.AppUserRepo;
import com.company.efood.sys.repository.ShopRepo;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import javax.naming.NameNotFoundException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@AllArgsConstructor
public class BranchServiceImpl implements BranchService {

    private final BranchRepo branchRepo;
    private final ShopRepo shopRepo;
    private final SellerRepo sellerRepo;
    private final AppUserRepo appUserRepo;
    private final BaseUtils baseUtils;
    private final ModelMapper modelMapper;

    private Shop resolveShop(Long sellerId, Long userId) {
        Shop shop = null;
        if (sellerId != null) {
            shop = shopRepo.findShopBySellerId(sellerId).orElse(null);
        }
        if (shop == null && userId != null) {
            Seller seller = sellerRepo.findByAppUserId(userId).orElse(null);
            if (seller != null) {
                shop = shopRepo.findShopBySellerId(seller.getId()).orElse(null);
            }
        }
        return shop;
    }

    @Override
    @Transactional
    public BranchDto save(BranchDto obj, Long userId) {
        Long sellerId = CurrentUserContext.getReferenceId();
        Branch branch = branchRepo.save(generateEntity(obj, userId, sellerId, true));
        return generateDto(branch);
    }

    @Override
    @Transactional
    public BranchDto update(BranchDto obj, Long userId) {
        Long sellerId = CurrentUserContext.getReferenceId();
        Branch existing = branchRepo.findById(obj.getId()).orElseThrow(() -> new RuntimeException("Branch not found"));
        Branch updated = generateEntity(obj, userId, sellerId, false);
        updated.setId(existing.getId());
        updated.setEntryUser(existing.getEntryUser());
        updated.setEntryDate(existing.getEntryDate());
        return generateDto(branchRepo.save(updated));
    }

    @Override
    @Transactional
    public boolean delete(BranchDto obj, Long userId) {
        if (obj.getId() != null) {
            branchRepo.deleteById(obj.getId());
            return true;
        }
        return false;
    }

    @Override
    public BranchDto getById(Long id, Long userId) {
        return branchRepo.findById(id).map(this::generateDto).orElse(null);
    }

    @Override
    public List<BaseDropdownModel> getDropdownList(Long userId) {
        return branchRepo.findAll().stream().map(branch -> new BaseDropdownModel() {
            @Override
            public Integer getId() {
                return Math.toIntExact(branch.getId());
            }

            @Override
            public String getName() {
                return branch.getName();
            }

            @Override
            public Integer getExtra() {
                return null;
            }

            @Override
            public String getExtraName() {
                return null;
            }

            @Override
            public String getExtraFromDate() {
                return null;
            }

            @Override
            public String getExtraToDate() {
                return null;
            }
        }).collect(Collectors.toList());
    }

    @Override
    public Page<BranchDto> getPageableAllData(BasePageableRequest pageableBodyRequest, Long userId) {
        PageRequest pageRequest = baseUtils.getPageRequest(pageableBodyRequest.getPage(), pageableBodyRequest.getSize());
        Page<Branch> branchPage = branchRepo.findAll(pageRequest);
        return new PageImpl<>(convertEntityListToDtoList(branchPage.stream()), pageRequest, branchPage.getTotalElements());
    }

    @Override
    public  Page<BranchDto> getPageableAllByUserLocation(BasePageableRequest pageableBodyRequest){
        PageRequest pageRequest = baseUtils.getPageRequest(pageableBodyRequest.getPage(),pageableBodyRequest.getSize());
        Page<Branch> categoryPage = branchRepo.findAll(pageRequest);
        return new PageImpl<>(convertEntityListToDtoList(categoryPage.stream()), pageRequest, categoryPage.getTotalElements());
    }

    @Override
    public List<BaseDropdownModel> getDropdownListByShopId(Long shopId, Long userId) {
        return branchRepo.findDropdownModelByShopId(shopId);
    }


    //-----------------------Helper Function---------------------------

    private Branch generateEntity(BranchDto dto, Long userId, Long sellerId, Boolean isSaved) {
        Logger logger = Logger.getLogger("BranchServiceImpl");
        Branch entity = new Branch();
        BeanUtils.copyProperties(dto, entity);
        try {
            Shop shop = resolveShop(sellerId, userId);
            if (shop == null && dto.getShopId() != null) {
                shop = shopRepo.findById(dto.getShopId().longValue()).orElse(null);
            }
            if (shop == null) {
                throw new NameNotFoundException("Shop Not Found for the authenticated seller");
            }

            if (isSaved) {
                entity.setEntryUser(userId);
                entity.setShop(shop);
                entity.setActive(true);
                logger.info("Branch: " + entity.getName());
                entity.setAddress(generateAddressEntity(dto.getAddress(), userId));
                baseUtils.setEntryUserInfo(entity);
            } else {
                Branch dbEntity = branchRepo.findById(dto.getId()).orElseThrow(() -> new NameNotFoundException("Branch not found"));
                entity.setShop(dbEntity.getShop() != null ? dbEntity.getShop() : shop);
                entity.setUpdateUser(userId);
                baseUtils.setUpdateUserInfo(entity, dbEntity);
            }
            return entity;
        } catch (Exception e) {
            throw new RuntimeException("Error generating Branch entity: " + e.getMessage(), e);
        }
    }


    private Address generateAddressEntity(AddressDto addressDto, Long userId) {
        Address address = new Address();
        BeanUtils.copyProperties(addressDto, address);
        address.setEntryDate(LocalDateTime.now());
        address.setEntryUser(userId);
        return address;
    }

    private List<BranchDto> convertEntityListToDtoList(Stream<Branch> entityList) {
        return entityList.map(this::generateDto).collect(Collectors.toList());
    }


    private BranchDto generateDto(Branch entity) {
        return modelMapper.map(entity, BranchDto.class);
    }







}

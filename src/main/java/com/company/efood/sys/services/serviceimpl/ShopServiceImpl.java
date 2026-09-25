package com.company.efood.sys.services.serviceimpl;

import com.company.efood.base.BaseDropdownModel;
import com.company.efood.base.BasePageableRequest;
import com.company.efood.base.BaseUtils;
import com.company.efood.config.CurrentUserContext;
import com.company.efood.seller.entity.Seller;
import com.company.efood.seller.repository.BranchRepo;
import com.company.efood.seller.repository.SellerRepo;
import com.company.efood.sys.dto.AddressDto;
import com.company.efood.sys.dto.ShopDto;
import com.company.efood.sys.entity.*;
import com.company.efood.sys.repository.AddressRepo;
import com.company.efood.sys.repository.AppUserRepo;
import com.company.efood.sys.repository.ShopRepo;
import com.company.efood.sys.services.ShopService;
import com.company.efood.zone.repository.ZoneRepository;
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
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@AllArgsConstructor
public class ShopServiceImpl implements ShopService {
    private final ShopRepo shopRepo;
    private final SellerRepo sellerRepo;
    private final AppUserRepo appUserRepo;
    private final ZoneRepository zoneRepository;
    private final BranchRepo branchRepo;
    private final AddressRepo addressRepo;
    private final BaseUtils baseUtils;
    private final ModelMapper modelMapper;

    private Seller resolveSeller(Long sellerId, Long userId) {
        Seller seller = null;
        if (sellerId != null) {
            seller = sellerRepo.findById(sellerId).orElse(null);
        }
        if (seller == null && userId != null) {
            seller = sellerRepo.findByAppUserId(userId).orElse(null);
        }
        if (seller == null && userId != null) {
            AppUser user = appUserRepo.findById(userId).orElse(null);
            if (user != null) {
                Seller newSeller = new Seller(user);
                newSeller.setFullName(user.getDisplayName());
                seller = sellerRepo.save(newSeller);
            }
        }
        return seller;
    }

    @Override
    @Transactional
    public ShopDto save(ShopDto obj, Long userId) {
        Long refId = CurrentUserContext.getReferenceId();
        Seller seller = resolveSeller(refId, userId);
        if (seller == null) {
            throw new RuntimeException("Seller profile not found for user ID: " + userId);
        }

        // Check if shop already exists for this seller to avoid duplicate shop creation
        Shop existingShop = shopRepo.findShopBySellerId(seller.getId()).orElse(null);
        if (existingShop != null) {
            obj.setId(existingShop.getId());
            return update(obj, userId);
        }

        Shop shopEntity = generateEntity(obj, userId, seller.getId(), true);
        shopEntity = shopRepo.save(shopEntity);

        // Ensure default branch is created so shop is immediately operational
        ensureDefaultBranch(shopEntity, userId);

        return generateDto(shopEntity);
    }

    @Override
    @Transactional
    public ShopDto update(ShopDto obj, Long userId) {
        Long refId = CurrentUserContext.getReferenceId();
        Seller seller = resolveSeller(refId, userId);
        Long sellerId = seller != null ? seller.getId() : refId;

        Shop existing = null;
        if (obj.getId() != null) {
            existing = shopRepo.findById(obj.getId()).orElse(null);
        }
        if (existing == null && sellerId != null) {
            existing = shopRepo.findShopBySellerId(sellerId).orElse(null);
        }
        if (existing == null) {
            throw new RuntimeException("Shop not found for update");
        }

        Shop updated = generateEntity(obj, userId, sellerId, false);
        updated.setId(existing.getId());
        updated.setSeller(existing.getSeller() != null ? existing.getSeller() : seller);
        updated.setEntryUser(existing.getEntryUser());
        updated.setEntryDate(existing.getEntryDate());
        Shop saved = shopRepo.save(updated);

        ensureDefaultBranch(saved, userId);

        return generateDto(saved);
    }

    private void ensureDefaultBranch(Shop shop, Long userId) {
        try {
            boolean hasBranch = branchRepo.findAll().stream()
                    .anyMatch(b -> b.getShop() != null && Objects.equals(b.getShop().getId(), shop.getId()));
            if (!hasBranch) {
                Branch defaultBranch = new Branch();
                defaultBranch.setName(shop.getShopName() + " - Main Branch");
                defaultBranch.setShop(shop);
                defaultBranch.setIsOpen(true);
                defaultBranch.setActive(true);
                defaultBranch.setEntryUser(userId);
                defaultBranch.setEntryDate(LocalDateTime.now());

                Address branchAddress = new Address();
                branchAddress.setAddress(shop.getShopAddress() != null ? shop.getShopAddress() : shop.getShopName());
                branchAddress.setLat(shop.getShopLat() != null ? shop.getShopLat() : 0.0);
                branchAddress.setLon(shop.getShopLon() != null ? shop.getShopLon() : 0.0);
                if (shop.getZone() != null) {
                    branchAddress.setZone(shop.getZone());
                    if (shop.getZone().getUpazila() != null) {
                        branchAddress.setUpazila(shop.getZone().getUpazila());
                        branchAddress.setPoliceStation(shop.getZone().getUpazila().getName());
                        if (shop.getZone().getUpazila().getDistrict() != null) {
                            branchAddress.setDistrict(shop.getZone().getUpazila().getDistrict().getName());
                        } else {
                            branchAddress.setDistrict("Dhaka");
                        }
                    } else {
                        branchAddress.setPoliceStation("Dhaka");
                        branchAddress.setDistrict("Dhaka");
                    }
                } else {
                    branchAddress.setDistrict("Dhaka");
                    branchAddress.setPoliceStation("Dhaka");
                }
                branchAddress.setEntryUser(userId);
                branchAddress.setEntryDate(LocalDateTime.now());
                branchAddress = addressRepo.save(branchAddress);

                defaultBranch.setAddress(branchAddress);
                branchRepo.save(defaultBranch);
            }
        } catch (Exception e) {
            System.err.println("Warning: could not auto-create default branch for shop: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public boolean delete(ShopDto obj, Long userId) {
        if (obj.getId() != null) {
            shopRepo.deleteById(obj.getId());
            return true;
        }
        return false;
    }

    @Override
    public ShopDto getById(Long id, Long userId) {
        return shopRepo.findById(id).map(this::generateDto).orElse(null);
    }

    @Override
    public ShopDto getMyShop(Long userId) {
        Long sellerId = CurrentUserContext.getReferenceId();
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
        if (shop == null) return null;
        return generateDto(shop);
    }

    @Override
    public List<BaseDropdownModel> getDropdownList(Long userId) {
        return shopRepo.findAll().stream().map(shop -> new BaseDropdownModel() {
            @Override
            public Integer getId() {
                return Math.toIntExact(shop.getId());
            }

            @Override
            public String getName() {
                return shop.getShopName();
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
    public Page<ShopDto> getPageableAllData(BasePageableRequest pageableBodyRequest, Long userId) {
        PageRequest pageRequest = baseUtils.getPageRequest(pageableBodyRequest.getPage(),pageableBodyRequest.getSize());
        Page<Shop> shopPage = shopRepo.findAll(pageRequest);
        List<ShopDto> shopList = convertEntityListToDtoList(shopPage.getContent().stream());
        return new PageImpl<>(shopList,pageRequest,shopPage.getTotalElements());
    }

    //-----------------------Helper Function---------------------------

    private Shop generateEntity(ShopDto dto, Long userId, Long sellerId, Boolean isSaved) {
        if (dto == null) {
            throw new RuntimeException("ShopDto is null");
        }

        Shop entity = new Shop();
        BeanUtils.copyProperties(dto, entity);

        entity.setShopLat(dto.getShopLat());
        entity.setShopLon(dto.getShopLon());
        entity.setShopAddress(dto.getShopAddress());
        entity.setShopType(dto.getShopType());

        if (dto.getZoneId() != null) {
            zoneRepository.findById(dto.getZoneId()).ifPresent(entity::setZone);
        }

        try {
            if (Boolean.TRUE.equals(isSaved)) {
                if (sellerId == null) {
                    throw new RuntimeException("sellerId cannot be null for saving new shop");
                }
                Seller seller = sellerRepo.findById(sellerId)
                        .orElseThrow(() -> new NameNotFoundException("Seller Not Found with ID: " + sellerId));

                entity.setEntryUser(userId);
                entity.setSeller(seller);
                baseUtils.setEntryUserInfo(entity);
            } else {
                if (dto.getId() != null) {
                    Shop dbEntity = shopRepo.findById(dto.getId()).orElse(null);
                    if (dbEntity != null) {
                        entity.setUpdateUser(userId);
                        baseUtils.setUpdateUserInfo(entity, dbEntity);
                        if (entity.getZone() == null && dbEntity.getZone() != null && dto.getZoneId() == null) {
                            entity.setZone(dbEntity.getZone());
                        }
                    }
                }
            }

            return entity;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error generating Shop entity. Root cause: " + e.getMessage(), e);
        }
    }

    private Address generateAddressEntity(AddressDto addressDto,Long userId) {
        Address address = new Address();
        BeanUtils.copyProperties(addressDto, address);
        address.setEntryDate(LocalDateTime.now());
        address.setEntryUser(userId);
        return address;
    }

    private List<ShopDto> convertEntityListToDtoList(Stream<Shop> entityList) {
        return entityList.map(this::generateDto).collect(Collectors.toList());
    }

    private ShopDto generateDto(Shop entity) {
        ShopDto dto = modelMapper.map(entity, ShopDto.class);
        if (entity.getSeller() != null) {
            dto.setSellerId(entity.getSeller().getId());
        }
        if (entity.getZone() != null) {
            dto.setZoneId(entity.getZone().getId());
        }
        if (entity.getCommissionPolicy() != null) {
            dto.setCommissionPolicyId(entity.getCommissionPolicy().getId());
        }
        dto.setShopLat(entity.getShopLat());
        dto.setShopLon(entity.getShopLon());
        dto.setShopAddress(entity.getShopAddress());
        dto.setShopType(entity.getShopType());
        return dto;
    }
}

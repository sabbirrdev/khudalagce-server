package com.company.efood.sys.services.serviceimpl;

import com.company.efood.base.BaseDropdownModel;
import com.company.efood.base.BasePageableRequest;
import com.company.efood.base.BaseUtils;
import com.company.efood.config.CurrentUserContext;
import com.company.efood.seller.repository.BranchRepo;
import com.company.efood.sys.dto.ProductAddonDto;
import com.company.efood.sys.dto.ProductDto;
import com.company.efood.sys.dto.ProductImageDto;
import com.company.efood.sys.dto.ProductVariantDto;
import com.company.efood.sys.entity.*;
import com.company.efood.sys.repository.CategoryRepo;
import com.company.efood.sys.repository.ProductRepo;
import com.company.efood.sys.services.ProductService;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.ObjectUtils;
import org.junit.platform.commons.logging.Logger;
import org.junit.platform.commons.logging.LoggerFactory;
import org.modelmapper.ModelMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@AllArgsConstructor
@Service
public class ProductServiceImpl implements ProductService {
    private ProductRepo productRepo;
    private CategoryRepo categoryRepo;
    private BranchRepo  branchRepo;
    private CurrentUserContext currentUserContext;
    private BaseUtils baseUtils;
    private ModelMapper modelMapper;

    @Override
    public ProductDto save(ProductDto obj, Long userId) {
        Product savedProduct = productRepo.save(generateEntity(obj, userId, true));
        return generateDto(savedProduct);
    }

    @Override
    public ProductDto update(ProductDto obj, Long userId) {
        return generateDto(productRepo.save(generateEntity(obj,userId,false)));
    }

    @Override
    public boolean delete(ProductDto obj, Long userId) {
        if (obj.getId() != null) {
            productRepo.deleteById(obj.getId());
            return true;
        }
        return false;
    }

    @Override
    public ProductDto getById(Long id, Long userId) {
        return productRepo.findById(id).map(this::generateDto).orElse(null);
    }

    @Override
    public List<BaseDropdownModel> getDropdownList(Long userId) {
        return productRepo.findAll().stream().map(product -> new BaseDropdownModel() {
            @Override
            public Integer getId() {
                return Math.toIntExact(product.getId());
            }

            @Override
            public String getName() {
                return product.getProductName();
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
    public Page<ProductDto> getPageableAllData(BasePageableRequest pageableBodyRequest, Long userId) {
        PageRequest pageRequest = baseUtils.getPageRequest(pageableBodyRequest.getPage(),pageableBodyRequest.getSize());
        Page<Product> productPage = productRepo.findAll(pageRequest);
        return new PageImpl<>(convertEntityListToDtoList(productPage.stream()), pageRequest, productPage.getTotalElements());
    }

    @Override
    public Page<ProductDto> getPaginatedProductsByBranch(BasePageableRequest basePageableRequest,Long branchId) {
        PageRequest pageRequest = baseUtils.getPageRequest(basePageableRequest.getPage(), basePageableRequest.getSize());
        Page<Product> productPage = productRepo.findPaginatedProductByBranchId(branchId, pageRequest);
        if (productPage.isEmpty()) {
            return new PageImpl<>(Collections.emptyList(), pageRequest, 0);
        }
        return new PageImpl<>( convertEntityListToDtoList(productPage.stream()) , pageRequest, productPage.getTotalElements());
    }

    @Override
    public List<ProductDto> getProductsByBranchAndCategory(Long branchId,Long categoryId) {
        return convertEntityListToDtoList(productRepo.findByBranchIdAndCategoryId(branchId, categoryId).stream());
    }

//    @Override
//    public Page<ProductDto> getPageableProductByBranch(BasePageableRequest basePageableRequest,Long branchId) {
//        PageRequest pageRequest = baseUtils.getPageRequest(basePageableRequest.getPage(), basePageableRequest.getSize());
//        String productType = normalizeOptionalFilter(basePageableRequest.getStringParam1());
//        String serviceType = normalizeOptionalFilter(basePageableRequest.getStringParam2());
//        Page<Product> productPage = productRepo.findPaginatedProductByBranchIdAndFilters(branchId, productType, serviceType, pageRequest);
//        return new PageImpl<>(convertEntityListToDtoList(productPage.stream()),pageRequest,productPage.getTotalElements());
//    }
@Override
public Page<ProductDto> getPageableProductByBranch(BasePageableRequest basePageableRequest, Long branchId) {
    try {
        PageRequest pageRequest = baseUtils.getPageRequest(basePageableRequest.getPage(), basePageableRequest.getSize());
        String productType = normalizeOptionalFilter(basePageableRequest.getStringParam1());
        String serviceType = normalizeOptionalFilter(basePageableRequest.getStringParam2());

        Page<Product> productPage = productRepo.findPaginatedProductByBranchIdAndFilters(branchId, productType, serviceType, pageRequest);

        return new PageImpl<>(convertEntityListToDtoList(productPage.stream()), pageRequest, productPage.getTotalElements());
    } catch (Exception e) {
        // This will print the exact line number and the real cause of the crash in your console
        e.printStackTrace();
        throw new RuntimeException("Error in getPageableProductByBranch: " + e.getMessage(), e);
    }
}


    @Override
    public List<ProductDto> getLowStockProductsByBranch(Long branchId, Integer threshold) {
        if (branchId == null) {
            throw new IllegalArgumentException("Branch ID is required for low-stock review");
        }
        Integer effectiveThreshold = threshold != null ? threshold : 5;
        return convertEntityListToDtoList(productRepo.findLowStockProductsByBranchId(branchId, effectiveThreshold).stream());
    }

    @Override
    public Page<ProductDto> getProductsWithFilters(Long branchId, Long categoryId, String productType, String serviceType, String search, int page, int size) {
        PageRequest pageRequest = baseUtils.getPageRequest(page, size > 0 ? size : 50);
        String pType = normalizeOptionalFilter(productType);
        String sType = normalizeOptionalFilter(serviceType);
        String q = (search != null && !search.isBlank()) ? search.trim() : null;

        Page<Product> productPage = productRepo.findActiveProductsWithFilters(branchId, categoryId, pType, sType, q, pageRequest);
        return new PageImpl<>(convertEntityListToDtoList(productPage.stream()), pageRequest, productPage.getTotalElements());
    }


//----------------------------Helper Function


    private Product generateEntity(ProductDto dto, Long userId, Boolean isSaved) {
        Product entity = new Product();
        BeanUtils.copyProperties(dto, entity);

        entity.setProductType(resolveProductType(dto.getProductType()));
        entity.setServiceType(resolveServiceType(dto.getServiceType()));
        entity.setQty(dto.getQty() != null ? dto.getQty() : 0);
        entity.setVat(dto.getVat() != null ? dto.getVat() : BigDecimal.ZERO);
        entity.setActive(true);
        setCategory(entity, dto.getCategoryId());
        setBranch(entity, dto.getBranchId());
        setProductVariants(entity, dto);
        setProductAddons(entity, dto);

        if (Boolean.TRUE.equals(isSaved)) {
            setEntryUserInfo(entity, userId);
        }
        else {
            updateFromExisting(entity, dto.getId(), userId);
        }

        return entity;
    }

    private String resolveProductType(String productType) {
        if (ObjectUtils.isEmpty(productType)) {
            return "GENERAL";
        }
        return productType.trim().toUpperCase();
    }

    private String resolveServiceType(String serviceType) {
        if (ObjectUtils.isEmpty(serviceType)) {
            return null;
        }
        return serviceType.trim().toUpperCase();
    }

    private String normalizeOptionalFilter(String filterValue) {
        if (ObjectUtils.isEmpty(filterValue) || filterValue.isBlank()) {
            return null;
        }
        return filterValue.trim().toUpperCase();
    }

    private List<ProductDto> convertEntityListToDtoList(Stream<Product> entityList) {
        return entityList.map(this::generateDto).collect(Collectors.toList());
    }


    public ProductDto generateDto(Product entity) {
        ProductDto dto = modelMapper.map(entity, ProductDto.class);
        if (entity.getBranch() != null) {
            dto.setBranchId(entity.getBranch().getId());
            dto.setBranchName(entity.getBranch().getName());
            if (entity.getBranch().getShop() != null) {
                dto.setShopId(entity.getBranch().getShop().getId());
                dto.setShopName(entity.getBranch().getShop().getShopName());
            }
        }
        return dto;
    }

    private void setCategory(Product entity, Long categoryId) {
        if (ObjectUtils.isEmpty(categoryId)) {
            throw new IllegalArgumentException("Category ID is required.");
        }
        Category category = categoryRepo.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Category not found: " + categoryId));
        entity.setCategory(category);
    }

    private void setBranch(Product entity, Long branchId) {
        if (ObjectUtils.isEmpty(branchId)) {
            throw new IllegalArgumentException("Branch ID must be provided because seller may have multiple branches.");
        }

        Branch branch = branchRepo.findById(branchId)
                .orElseThrow(() -> new IllegalArgumentException("Branch not found: " + branchId));

        entity.setBranch(branch);
    }

    private  void setProductAddons(Product entity, ProductDto dto) {
        // Addons
        List<ProductAddon> addons = dto.getAddons() != null ?
                dto.getAddons().stream().map(addonDto -> {
                    ProductAddon addon = new ProductAddon();
                    addon.setAddonName(addonDto.getAddonName());
                    addon.setPrice(addonDto.getPrice());
                    addon.setProduct(entity); // important!
                    return addon;
                }).collect(Collectors.toList()) : new ArrayList<>();
        entity.setAddons(addons);
    }
    private void setProductVariants(Product entity, ProductDto dto) {
        // Variants
        List<ProductVariant> variants = dto.getVariants() != null ?
                dto.getVariants().stream().map(variantDto -> {
                    ProductVariant variant = new ProductVariant();
                    variant.setVariantName(variantDto.getVariantName());
                    variant.setExtraPrice(variantDto.getExtraPrice());
                    variant.setProduct(entity); // important!
                    return variant;
                }).collect(Collectors.toList()) : new ArrayList<>();
        entity.setVariants(variants);
    }

    private void setEntryUserInfo(Product entity, Long userId) {
        entity.setEntryUser(userId);
        baseUtils.setEntryUserInfo(entity);
    }

    private void updateFromExisting(Product entity, Long productId, Long userId) {
        Product existing = productRepo.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));
        entity.setUpdateUser(userId);
        baseUtils.setUpdateUserInfo(entity, existing);
    }



}

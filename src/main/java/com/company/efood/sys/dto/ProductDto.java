package com.company.efood.sys.dto;

import com.company.efood.base.BaseDto;
import com.company.efood.seller.dto.BranchDto;
import com.company.efood.sys.entity.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
public class ProductDto extends BaseDto {
    private String productName;
    private String productType = "GENERAL";
    private String serviceType;
    private String imgUrl;
    private List<ProductImageDto> images;
    private String description;
    private BigDecimal price;
    private BigDecimal buyingPrice;
    private BigDecimal discountPrice;
    private String itemWeight;
    private String brand = "No Brand";
    private Integer qty;
    private BigDecimal vat ;

    private Long categoryId;
    private String categoryName;

    private Long branchId;
    private String branchName;

    private Long shopId;
    private String shopName;

    private Long reviewId;
    private Integer reviewCount;
    private BigDecimal rating;

    private List<ProductVariantDto> variants;
    private List<ProductAddonDto> addons;
    private Integer preparationTime; // in minutes
    private boolean isFeatured = false;
    private boolean isTopRated = false;
    private boolean isPopular = false;
    private List<String> tags;
}

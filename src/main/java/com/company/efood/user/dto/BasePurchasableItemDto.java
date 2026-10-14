package com.company.efood.user.dto;

import com.company.efood.base.BaseDto;
import com.company.efood.sys.dto.ProductAddonDto;
import com.company.efood.sys.dto.ProductDto;
import com.company.efood.sys.entity.Product;
import com.company.efood.sys.entity.ProductAddon;
import com.company.efood.sys.entity.ProductVariant;
import jakarta.persistence.Column;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.List;

@Data
@MappedSuperclass
@EqualsAndHashCode(callSuper = true)
public abstract class BasePurchasableItemDto extends BaseDto {
    private ProductDto product;
    private List<ProductAddonDto> addons;
    private Integer  quantity;
    private Long variantId;
    private BigDecimal unitPrice;
    private BigDecimal buyingPrice;
    private BigDecimal totalPrice;

}

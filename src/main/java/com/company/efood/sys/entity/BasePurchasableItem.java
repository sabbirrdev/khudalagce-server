package com.company.efood.sys.entity;

import com.company.efood.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@MappedSuperclass
@EqualsAndHashCode(callSuper = true)
public abstract class BasePurchasableItem extends BaseEntity {

    @ManyToOne
    @JoinColumn(name = "PRODUCT_ID", nullable = false)
    private Product product;

    @Column(name = "QUANTITY", nullable = false)
    private Integer  quantity;

    @ManyToOne
    @JoinColumn(name = "VARIANT_ID")
    private ProductVariant variant;

    @Column(name = "UNIT_PRICE", nullable = false)
    private BigDecimal unitPrice;

    @Column(name = "BUYING_PRICE", precision = 19, scale = 4)
    private BigDecimal buyingPrice;

    @Column(name = "TOTAL_PRICE", nullable = false)
    private BigDecimal totalPrice;


}

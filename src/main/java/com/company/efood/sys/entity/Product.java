package com.company.efood.sys.entity;

import com.company.efood.base.BaseEntity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.math.BigDecimal;
import java.util.List;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@Table(name = "PRODUCT")
public class Product extends BaseEntity {

    @Column(name = "PRODUCT_NAME", nullable = false)
    private String productName;

    @Column(name = "PRODUCT_TYPE")
    private String productType = "GENERAL";

    @Column(name = "SERVICE_TYPE")
    private String serviceType;

    @Column(name = "IMAGE_URL", nullable = false)
    private String imgUrl;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductImage> images;

    @Column(name = "DESCRIPTION", columnDefinition = "text")
    private String description;

    @Column(name = "PRICE", nullable = false)
    private BigDecimal price;

    @Column(name = "BUYING_PRICE", precision = 19, scale = 4)
    private BigDecimal buyingPrice;

    @Column(name = "DISCOUNT_PRICE")
    private BigDecimal discountPrice;

    @Column(name = "ITEM_WEIGHT")
    private String itemWeight; // e.g. "500g", "1 kg", "250 ml", "1 L"

    @Column(name = "VAT", nullable = false)
    private BigDecimal vat = BigDecimal.valueOf(0.0);

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CATEGORY_ID", nullable = false)
    private Category category;

    @Column(name = "BRAND")
    private String brand;

    @Column(name = "QTY", nullable = false)
    private Integer qty = 0;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "BRANCH_ID", nullable = false)
    private Branch branch;

    @JsonIgnore
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Review> reviewList;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductVariant> variants;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductAddon> addons;

    @Column(name = "PREPARATION_TIME")
    private Integer preparationTime; // in minutes

    @Column(name = "IS_FEATURED",insertable = false )
    private Boolean isFeatured = false;

    @Column(name = "IS_TOP_RATED",insertable = false)
    private Boolean isTopRated = false;

    @Column(name = "IS_POPULAR",insertable = false)
    private Boolean isPopular = false;

    @ElementCollection
    @CollectionTable(name = "PRODUCT_TAGS", joinColumns = @JoinColumn(name = "PRODUCT_ID"))
    @Column(name = "TAG")
    private List<String> tags;

}

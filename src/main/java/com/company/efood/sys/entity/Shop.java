package com.company.efood.sys.entity;

import com.company.efood.base.BaseEntity;
import com.company.efood.seller.entity.Seller;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@Entity
@EqualsAndHashCode(callSuper = true)
@Table(name = "SHOP")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Shop extends BaseEntity {
    @Column(name = "SHOP_NAME", nullable = false, length = 50)
    private String shopName;

    @Column(name = "PHONE_NUMBER", nullable = false)
    private String phoneNumber;

    @Column(name = "SHOP_NAME_BN", length = 50)
    private String shopNameBn;

    @Column(name = "ABOUT")
    private String about;

    @Column(name = "LOGO_URL")
    private String logoUrl;

    @JsonIgnore
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "SELLER_ID", nullable = false)
    private Seller seller;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "COMMISSION_POLICY_ID")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private CommissionPolicy commissionPolicy;

    @JsonIgnore
    @OneToMany(mappedBy = "shop", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Review> reviewList;

    @JsonIgnore
    @OneToMany(mappedBy = "shop", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Branch> branches;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ZONE_ID")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private com.company.efood.zone.entity.Zone zone;

    @Column(name = "SHOP_LAT")
    private Double shopLat;

    @Column(name = "SHOP_LON") 
    private Double shopLon;

    @Column(name = "SHOP_ADDRESS")
    private String shopAddress;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "SHOP_CATEGORY_ID")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private ShopCategory shopCategory;

    @Column(name = "SHOP_TYPE")
    private String shopType;
}

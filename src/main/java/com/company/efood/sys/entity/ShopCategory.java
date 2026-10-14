package com.company.efood.sys.entity;

import com.company.efood.base.BaseEntity;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Shop Category entity (e.g. Restaurant, Pharmacy, Hardware, Grocery Store).
 * Relationships:
 * - Many-to-One to AppService (parental Service/Vertical)
 * - Many-to-One to CommissionPolicy (multiple categories can share the same policy)
 */
@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@Table(name = "SHOP_CATEGORY")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ShopCategory extends BaseEntity {

    @Column(name = "NAME", nullable = false, length = 100)
    private String name;

    @Column(name = "NAME_BN", length = 100)
    private String nameBn;

    @Column(name = "CODE", length = 50)
    private String code;

    @Column(name = "DESCRIPTION", length = 500)
    private String description;

    @Column(name = "ICON", length = 100)
    private String icon;

    @Column(name = "IMAGE_URL", length = 300)
    private String imageUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "SERVICE_ID", nullable = false)
    @JsonIgnoreProperties({"shopCategories", "hibernateLazyInitializer", "handler"})
    private AppService service;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "COMMISSION_POLICY_ID")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private CommissionPolicy commissionPolicy;
}

package com.company.efood.sys.entity;

import com.company.efood.base.BaseEntity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

/**
 * Service (Vertical) entity for dynamic service management
 * (e.g. Food Delivery, Pharmacy, Groceries, Ride Sharing, Exclusive Offers).
 */
@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@Table(name = "APP_SERVICE")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class AppService extends BaseEntity {

    @Column(name = "NAME", nullable = false, length = 100)
    private String name;

    @Column(name = "NAME_BN", length = 100)
    private String nameBn;

    @Column(name = "CODE", unique = true, nullable = false, length = 50)
    private String code;

    @Column(name = "DESCRIPTION", length = 500)
    private String description;

    @Column(name = "ICON", length = 100)
    private String icon;

    @Column(name = "COLOR", length = 30)
    private String color;

    @Column(name = "GRADIENT_COLORS", length = 100)
    private String gradientColors;

    @Column(name = "DISPLAY_ORDER")
    private Integer displayOrder = 0;

    @Column(name = "IS_ALWAYS_OPEN")
    private Boolean isAlwaysOpen = true;

    @JsonIgnore
    @OneToMany(mappedBy = "service", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ShopCategory> shopCategories = new ArrayList<>();
}

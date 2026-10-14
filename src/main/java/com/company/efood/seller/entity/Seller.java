package com.company.efood.seller.entity;

import com.company.efood.base.BaseEntity;
import com.company.efood.sys.entity.Address;
import com.company.efood.sys.entity.AppUser;
import com.company.efood.sys.entity.Shop;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.util.Date;

@Data
@Table(name = "SELLER")
@EqualsAndHashCode(callSuper = true)
@Entity
@NoArgsConstructor
@Getter
@Setter
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Seller extends BaseEntity {

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    @JsonIgnoreProperties({"seller", "password", "hibernateLazyInitializer", "handler"})
    private AppUser appUser;

    @Column(name = "FULL_NAME")
    private String fullName;
    @Column(name = "PHONE")
    private String phone;
    @Column(name = "NID")
    private String nid;
    @Column(name = "NID_FORNT")
    private String nidFront;
    @Column(name = "NID_BACK")
    private String nidBack;
    @Column(name = "OTHER_DOC")
    private String otherDoc;
    @Column(name = "BIRTH_DATE")
    private Date birthDate;

    @OnDelete(action = OnDeleteAction.CASCADE)
    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "ADDRESS_ID")
    private Address address;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "SHOP_CATEGORY_ID")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private com.company.efood.sys.entity.ShopCategory shopCategory;

    @OneToOne(mappedBy = "seller", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private Shop shop;

    public Seller(AppUser appUser) {
        this.appUser = appUser;
        this.setEntryDate(appUser.getEntryDate());
        this.setEntryUser(appUser.getEntryUser());
    }
}

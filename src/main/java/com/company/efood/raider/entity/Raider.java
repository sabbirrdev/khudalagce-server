package com.company.efood.raider.entity;

import com.company.efood.base.BaseEntity;
import com.company.efood.sys.entity.Address;
import com.company.efood.sys.entity.AppUser;
import com.company.efood.sys.entity.Shop;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.util.Date;
import java.util.List;

@Data
@Table(name = "RAIDER")
@EqualsAndHashCode(callSuper = true)
@Entity
@NoArgsConstructor
public class Raider extends BaseEntity {

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
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

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "RAIDER_DOCUMENTS", joinColumns = @JoinColumn(name = "RAIDER_ID"))
    @MapKeyColumn(name = "DOC_NAME")
    @Column(name = "DOC_URL", length = 1000)
    private java.util.Map<String, String> complianceDocuments = new java.util.HashMap<>();

    @Column(name = "VEHICLE_TYPE")
    private String vehicleType;

    @Column(name = "VEHICLE_NUMBER")
    private String vehicleNumber;

    @Column(name = "IS_AVAILABLE")
    private Boolean isAvailable = true;

    @Column(name = "CURRENT_LAT")
    private Double currentLat;

    @Column(name = "CURRENT_LNG")
    private Double currentLng;

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "ADDRESS_ID")
    private Address address;

    public Raider(AppUser appUser) {
        this.appUser = appUser;
        this.setEntryDate(appUser.getEntryDate());
        this.setEntryUser(appUser.getEntryUser());
    }
}

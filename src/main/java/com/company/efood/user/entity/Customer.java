package com.company.efood.user.entity;

import com.company.efood.base.BaseEntity;
import com.company.efood.sys.entity.Address;
import com.company.efood.sys.entity.AppUser;
import com.company.efood.sys.utils.Gender;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.util.Date;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@Table(name = "CUSTOMER", indexes = {
        @Index(name = "idx_customer_user_id", columnList = "user_id"),
        @Index(name = "idx_customer_referral_code", columnList = "referral_code")
})
@NoArgsConstructor
public class Customer extends BaseEntity {

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private AppUser appUser;

    @Column(name = "FULL_NAME")
    private String fullName;
    @Column(name = "EMAIL")
    private String email;
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

    @Enumerated(EnumType.STRING)
    @Column(name = "GENDER")
    private Gender gender;

    @OnDelete(action = OnDeleteAction.CASCADE)
    @OneToOne()
    @JoinColumn(name = "ADDRESS")
    private Address address;

    // Referral Program
    @Column(name = "referral_code", unique = true, length = 12)
    private String referralCode;

    /** The customer who referred this customer (null if no referral was used). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "referred_by_id")
    private Customer referredBy;

    /** True once the referral reward has been credited (after first COMPLETED order). */
    @Column(name = "referral_reward_credited", nullable = false)
    private boolean referralRewardCredited = false;

    public Customer(AppUser appUser) {
        this.appUser = appUser;
        this.setEntryDate(appUser.getEntryDate());
        this.setEntryUser(appUser.getEntryUser());
    }
}
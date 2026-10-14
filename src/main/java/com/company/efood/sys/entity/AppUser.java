package com.company.efood.sys.entity;

import com.company.efood.raider.entity.Raider;
import com.company.efood.seller.entity.Employee;
import com.company.efood.user.entity.Customer;
import com.company.efood.seller.entity.Seller;
import com.company.efood.sys.utils.AppUserType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.company.efood.base.BaseEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * @version 1.0.0
 * @Author Md. Sabbir Hossain
 * @Email sabbirr883@gmail.com
 * @Since March 1, 2024
 */

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "SYA_APP_USER")
public class AppUser extends BaseEntity {
    private static final long serialVersionUID = 1L;

    @Column(name = "USERNAME", length = 50, nullable = false, unique = true)
    private String username;

    @Column(name = "DISPLAY_NAME", length = 100)
    private String displayName;

    @JsonIgnore
    @Column(name = "PASSWORD", nullable = false)
    private String password;

    @ManyToOne
    @JoinColumn(name = "PASSWORD_POLICY_ID", nullable = false)
    private PasswordPolicy passwordPolicy;

    @Column(name = "USER_TYPE_ID",nullable = false)
    private Integer userTypeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "APP_USER_TYPE",nullable = false)
    private AppUserType appUserType;

    @Column(name = "IS_ACCOUNT_EXPIRED", columnDefinition = "boolean default false")
    private Boolean accountExpired = false;

    @Column(name = "IS_CREDENTIALS_EXPIRED", columnDefinition = "boolean default false")
    private Boolean credentialsExpired = false;

    @Column(name = "IS_ACCOUNT_LOCKED", columnDefinition = "boolean default false")
    private Boolean accountLocked = false;

    @OneToOne(mappedBy = "appUser", cascade = CascadeType.ALL)
    private Seller seller;

    @OneToOne(mappedBy = "appUser", cascade = CascadeType.ALL)
    private Customer customer;

    @OneToOne(mappedBy = "appUser", cascade = CascadeType.ALL)
    private SystemAdmin admin;

    @OneToOne(mappedBy = "appUser", cascade = CascadeType.ALL)
    private Raider raider;

    @OneToOne(mappedBy = "appUser", cascade = CascadeType.ALL)
    private Employee employee;

    @Column(name = "OTP")
    private String otp;

    @Column(name = "OTP_EXPIRES_AT")
    private LocalDateTime otpExpiresAt;

    @Column(name = "PHONE", length = 30)
    private String phone;

    @Column(name = "PHONE_VERIFIED", columnDefinition = "boolean default false")
    private Boolean phoneVerified = false;

    @Column(name = "EMAIL_VERIFIED", columnDefinition = "boolean default false")
    private Boolean emailVerified = false;

}

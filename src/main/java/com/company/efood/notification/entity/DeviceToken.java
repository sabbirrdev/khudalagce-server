package com.company.efood.notification.entity;

import com.company.efood.base.BaseEntity;
import com.company.efood.sys.utils.AppUserType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "DEVICE_TOKENS")
@EqualsAndHashCode(callSuper = true)
public class DeviceToken extends BaseEntity {

    @Column(name = "APP_USER_ID", nullable = false)
    private Long appUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "APP_USER_TYPE", nullable = false)
    private AppUserType appUserType;

    @Column(name = "FCM_TOKEN", nullable = false, length = 1000)
    private String fcmToken;

    @Column(name = "DEVICE_TYPE")
    private String deviceType; // ANDROID, IOS, WEB

    @Column(name = "LAST_ACTIVE_AT")
    private LocalDateTime lastActiveAt;
}

package com.company.efood.notification.repository;

import com.company.efood.notification.entity.UserDeviceToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserDeviceTokenRepo extends JpaRepository<UserDeviceToken, Long> {

    Optional<UserDeviceToken> findByFcmToken(String fcmToken);

    List<UserDeviceToken> findByUserIdAndAppTypeAndIsActiveTrue(Long userId, UserDeviceToken.AppType appType);

    List<UserDeviceToken> findByUserIdAndIsActiveTrue(Long userId);

    List<UserDeviceToken> findByAppTypeAndIsActiveTrue(UserDeviceToken.AppType appType);

    void deleteByFcmToken(String fcmToken);
}

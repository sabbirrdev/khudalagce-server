package com.company.efood.notification.repository;

import com.company.efood.notification.entity.NotificationLog;
import com.company.efood.sys.utils.AppUserType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationLogRepo extends JpaRepository<NotificationLog, Long> {

    Page<NotificationLog> findByAppUserIdOrderByEntryDateDesc(Long appUserId, Pageable pageable);

    @org.springframework.data.jpa.repository.Query("SELECT n FROM NotificationLog n WHERE n.appUserId = :appUserId OR (n.appUserId IS NULL AND n.appUserType = :appUserType) ORDER BY n.entryDate DESC")
    Page<NotificationLog> findByAppUserIdOrAppUserTypeBroadcast(
            @org.springframework.data.repository.query.Param("appUserId") Long appUserId,
            @org.springframework.data.repository.query.Param("appUserType") AppUserType appUserType,
            Pageable pageable
    );

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(n) FROM NotificationLog n WHERE (n.appUserId = :appUserId OR (n.appUserId IS NULL AND n.appUserType = :appUserType)) AND (n.isRead = false OR n.isRead IS NULL)")
    long countUnreadForUserOrBroadcast(
            @org.springframework.data.repository.query.Param("appUserId") Long appUserId,
            @org.springframework.data.repository.query.Param("appUserType") AppUserType appUserType
    );

    Page<NotificationLog> findByAppUserTypeOrderByEntryDateDesc(AppUserType appUserType, Pageable pageable);

    Page<NotificationLog> findByNotificationTypeOrderByEntryDateDesc(String notificationType, Pageable pageable);

    Page<NotificationLog> findAllByOrderByEntryDateDesc(Pageable pageable);

    long countByAppUserIdAndIsReadFalse(Long appUserId);

    long countByAppUserTypeAndIsReadFalse(AppUserType appUserType);

    List<NotificationLog> findByOrderIdOrderByEntryDateDesc(Long orderId);
}

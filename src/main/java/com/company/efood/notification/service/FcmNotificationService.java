package com.company.efood.notification.service;

import com.company.efood.notification.dto.DeviceTokenDto;
import com.company.efood.notification.entity.DeviceToken;
import com.company.efood.notification.entity.NotificationLog;
import com.company.efood.notification.entity.UserDeviceToken;
import com.company.efood.notification.repository.DeviceTokenRepo;
import com.company.efood.notification.repository.NotificationLogRepo;
import com.company.efood.notification.repository.UserDeviceTokenRepo;
import com.company.efood.sys.utils.AppUserType;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class FcmNotificationService {

    private final DeviceTokenRepo deviceTokenRepo;
    private final UserDeviceTokenRepo userDeviceTokenRepo;
    private final NotificationLogRepo notificationLogRepo;

    public DeviceToken registerToken(Long userId, AppUserType userType, DeviceTokenDto dto) {
        Optional<DeviceToken> existing = deviceTokenRepo.findByFcmToken(dto.getFcmToken());
        DeviceToken token = existing.orElseGet(DeviceToken::new);
        token.setAppUserId(userId);
        token.setAppUserType(userType);
        token.setFcmToken(dto.getFcmToken());
        token.setDeviceType(dto.getDeviceType() != null ? dto.getDeviceType() : "ANDROID");
        token.setLastActiveAt(LocalDateTime.now());
        token.setActive(true);
        token.setIsApproved(false);
        if (token.getEntryDate() == null) {
            token.setEntryDate(LocalDateTime.now());
            token.setEntryUser(userId != null ? userId : 0L);
        }
        return deviceTokenRepo.save(token);
    }

    public void unregisterToken(String fcmToken) {
        deviceTokenRepo.deleteByFcmToken(fcmToken);
        userDeviceTokenRepo.deleteByFcmToken(fcmToken);
    }

    /**
     * Reusable notification sender by userId and AppType (CUSTOMER, RIDER, SELLER)
     */
    public void sendNotification(Long userId, UserDeviceToken.AppType appType, String title, String body, Map<String, String> data) {
        List<UserDeviceToken> tokens = userDeviceTokenRepo.findByUserIdAndAppTypeAndIsActiveTrue(userId, appType);
        
        AppUserType aut = switch (appType) {
            case CUSTOMER -> AppUserType.CUSTOMER;
            case RIDER -> AppUserType.RAIDER;
            case SELLER -> AppUserType.SELLER;
        };

        if (tokens.isEmpty()) {
            // Check in legacy table
            sendNotificationToUser(userId, aut, title, body, data, "GENERAL_ALERT", null);
            return;
        }

        for (UserDeviceToken token : tokens) {
            sendFcmMessage(token.getFcmToken(), title, body, data);
        }
    }

    public void sendNotificationToUser(Long userId, AppUserType userType, String title, String body, Map<String, String> data, String notificationType, Long orderId) {
        List<DeviceToken> tokens = deviceTokenRepo.findByAppUserIdAndActiveTrue(userId);
        List<UserDeviceToken> udtTokens = userDeviceTokenRepo.findByUserIdAndIsActiveTrue(userId);

        if (tokens.isEmpty() && udtTokens.isEmpty()) {
            log.info("📢 [FCM] No registered device tokens for userId={}, logged to database", userId);
            saveNotificationLog(userId, userType, title, body, notificationType, orderId, "NO_TOKEN", null);
            return;
        }

        Set<String> allTokens = new HashSet<>();
        for (DeviceToken dt : tokens) allTokens.add(dt.getFcmToken());
        for (UserDeviceToken udt : udtTokens) allTokens.add(udt.getFcmToken());

        String lastStatus = "PENDING";
        String lastMsgId = null;
        for (String fcmToken : allTokens) {
            String msgId = sendFcmMessage(fcmToken, title, body, data);
            if (msgId != null) {
                lastStatus = "SENT";
                lastMsgId = msgId;
            } else {
                lastStatus = "FAILED";
            }
        }
        saveNotificationLog(userId, userType, title, body, notificationType, orderId, lastStatus, lastMsgId);
    }

    public void sendNotificationToRiders(String title, String body, Map<String, String> data, String notificationType, Long orderId) {
        List<DeviceToken> riderTokens = deviceTokenRepo.findByAppUserTypeAndActiveTrue(AppUserType.RAIDER);
        List<UserDeviceToken> udtRiders = userDeviceTokenRepo.findByAppTypeAndIsActiveTrue(UserDeviceToken.AppType.RIDER);

        Set<String> allTokens = new HashSet<>();
        for (DeviceToken dt : riderTokens) allTokens.add(dt.getFcmToken());
        for (UserDeviceToken udt : udtRiders) allTokens.add(udt.getFcmToken());

        log.info("📢 [FCM] Broadcasting notification to {} active riders: '{}'", allTokens.size(), title);

        // Save a single broadcast notification log (appUserId=null signals broadcast)
        saveNotificationLog(null, AppUserType.RAIDER, title, body, notificationType, orderId,
                allTokens.isEmpty() ? "NO_RIDERS" : "SENT", null);

        for (String token : allTokens) {
            sendFcmMessage(token, title, body, data);
        }
    }

    /**
     * Saves a NotificationLog record with all required BaseEntity fields set.
     * This prevents NOT NULL constraint violations on ENTRY_USER, ENTRY_DATE, and ACTIVE columns.
     */
    private void saveNotificationLog(Long appUserId, AppUserType userType, String title, String body,
                                      String notificationType, Long orderId, String status, String fcmMessageId) {
        try {
            NotificationLog log2 = new NotificationLog();
            log2.setAppUserId(appUserId);
            log2.setAppUserType(userType);
            log2.setTitle(title);
            log2.setBody(body);
            log2.setNotificationType(notificationType);
            log2.setOrderId(orderId);
            log2.setStatus(status);
            log2.setFcmMessageId(fcmMessageId);
            log2.setIsRead(false);
            // BaseEntity required fields
            log2.setActive(true);
            log2.setIsApproved(false);
            log2.setEntryDate(LocalDateTime.now());
            log2.setEntryUser(appUserId != null ? appUserId : 0L);
            notificationLogRepo.save(log2);
        } catch (Exception ex) {
            log.error("⚠️ [FCM] Failed to save notification log: {}", ex.getMessage());
        }
    }

    public void notifySellerNewOrder(Long sellerAppUserId, Long orderId, String customerName, BigDecimal totalAmount) {
        String title = "🛒 New Order Received!";
        String body = String.format("Order #%d from %s (৳%.2f). Please accept and prepare.",
                orderId,
                customerName != null && !customerName.isBlank() ? customerName : "Customer",
                totalAmount != null ? totalAmount.doubleValue() : 0.0);

        Map<String, String> data = new HashMap<>();
        data.put("orderId", String.valueOf(orderId));
        data.put("type", "NEW_ORDER");
        data.put("click_action", "FLUTTER_NOTIFICATION_CLICK");

        sendNotificationToUser(sellerAppUserId, AppUserType.SELLER, title, body, data, "NEW_ORDER", orderId);
    }

    public void notifyRidersOrderPreparing(Long orderId, String shopName, String area) {
        String title = "🔔 Order Preparing - Delivery Soon!";
        String body = String.format("Order #%d at %s is being prepared. Get ready for pickup!",
                orderId, shopName != null ? shopName : "Restaurant");

        Map<String, String> data = new HashMap<>();
        data.put("orderId", String.valueOf(orderId));
        data.put("type", "ORDER_PREPARING");
        data.put("click_action", "FLUTTER_NOTIFICATION_CLICK");

        sendNotificationToRiders(title, body, data, "ORDER_PREPARING", orderId);
    }

    public void notifyRiderOrderAssigned(Long riderAppUserId, Long orderId, String shopName) {
        String title = "🛵 Order Assigned to You!";
        String body = String.format("Order #%d from %s is assigned to you. Please collect and deliver.",
                orderId, shopName != null ? shopName : "Restaurant");

        Map<String, String> data = new HashMap<>();
        data.put("orderId", String.valueOf(orderId));
        data.put("type", "ORDER_ASSIGNED");
        data.put("click_action", "FLUTTER_NOTIFICATION_CLICK");

        sendNotificationToUser(riderAppUserId, AppUserType.RAIDER, title, body, data, "ORDER_ASSIGNED", orderId);
    }

    public void notifyCustomerOrderStatus(Long customerAppUserId, Long orderId, String status, String message) {
        String title = "📦 Order Update: " + status;
        Map<String, String> data = new HashMap<>();
        data.put("orderId", String.valueOf(orderId));
        data.put("status", status);
        data.put("type", "ORDER_STATUS_UPDATE");
        data.put("click_action", "FLUTTER_NOTIFICATION_CLICK");

        sendNotificationToUser(customerAppUserId, AppUserType.CUSTOMER, title, message, data, "ORDER_STATUS_UPDATE", orderId);
    }

    public void notifyAdminNewRegistration(String userType, String displayName, String phone, Long newUserId) {
        String title = "👤 New " + userType + " Registered";
        String body = String.format("%s (%s) just registered. Please review and approve in Admin Portal.",
                displayName != null ? displayName : "New User",
                phone != null ? phone : "No Phone");

        Map<String, String> data = new HashMap<>();
        data.put("userId", String.valueOf(newUserId));
        data.put("userType", userType);
        data.put("type", "NEW_REGISTRATION");

        List<DeviceToken> adminTokens = deviceTokenRepo.findByAppUserTypeAndActiveTrue(AppUserType.SYSTEM_ADMIN);
        
        NotificationLog notifLog = NotificationLog.builder()
                .appUserType(AppUserType.SYSTEM_ADMIN)
                .title(title)
                .body(body)
                .notificationType("NEW_REGISTRATION")
                .status(adminTokens.isEmpty() ? "SAVED_FOR_ADMIN" : "SENT")
                .isRead(false)
                .build();
        notifLog.setEntryDate(LocalDateTime.now());
        notifLog.setEntryUser(newUserId != null ? newUserId : 0L);
        notificationLogRepo.save(notifLog);

        for (DeviceToken token : adminTokens) {
            sendFcmMessage(token.getFcmToken(), title, body, data);
        }
    }

    public void notifyRidersOrderReadyForPickup(Long orderId, String shopName, String area) {
        String title = "🛵 Order Ready for Pickup!";
        String body = String.format("Order #%d at %s is ready for pickup! Tap to claim and deliver.",
                orderId, shopName != null ? shopName : "Restaurant");

        Map<String, String> data = new HashMap<>();
        data.put("orderId", String.valueOf(orderId));
        data.put("type", "ORDER_READY_FOR_PICKUP");
        data.put("click_action", "FLUTTER_NOTIFICATION_CLICK");

        sendNotificationToRiders(title, body, data, "ORDER_READY_FOR_PICKUP", orderId);
    }

    /**
     * Admin-initiated broadcast notification to ALL customer devices.
     * Use for announcements, exclusive offers, or any app-wide notice.
     */
    public void sendBroadcastToAllCustomers(String title, String body, String notificationType) {
        List<DeviceToken> customerTokens = deviceTokenRepo.findByAppUserTypeAndActiveTrue(AppUserType.CUSTOMER);
        List<UserDeviceToken> udtCustomers = userDeviceTokenRepo.findByAppTypeAndIsActiveTrue(UserDeviceToken.AppType.CUSTOMER);

        Set<String> allTokens = new HashSet<>();
        for (DeviceToken dt : customerTokens) allTokens.add(dt.getFcmToken());
        for (UserDeviceToken udt : udtCustomers) allTokens.add(udt.getFcmToken());

        log.info("📢 [FCM] Admin broadcast to {} customer devices: '{}'", allTokens.size(), title);

        Map<String, String> data = new HashMap<>();
        data.put("type", notificationType != null ? notificationType : "ADMIN_BROADCAST");
        data.put("click_action", "FLUTTER_NOTIFICATION_CLICK");

        // Save a single broadcast log entry
        saveNotificationLog(null, AppUserType.CUSTOMER, title, body,
                notificationType != null ? notificationType : "ADMIN_BROADCAST",
                null, allTokens.isEmpty() ? "NO_CUSTOMERS" : "SENT", null);

        for (String token : allTokens) {
            sendFcmMessage(token, title, body, data);
        }
    }

    private String sendFcmMessage(String fcmToken, String title, String body, Map<String, String> data) {
        try {
            Notification notification = Notification.builder()
                    .setTitle(title)
                    .setBody(body)
                    .build();

            Message.Builder messageBuilder = Message.builder()
                    .setToken(fcmToken)
                    .setNotification(notification);

            if (data != null && !data.isEmpty()) {
                messageBuilder.putAllData(data);
            }

            // High-importance Android channel for immediate heads-up & audible alerts
            com.google.firebase.messaging.AndroidConfig androidConfig = com.google.firebase.messaging.AndroidConfig.builder()
                    .setPriority(com.google.firebase.messaging.AndroidConfig.Priority.HIGH)
                    .setNotification(com.google.firebase.messaging.AndroidNotification.builder()
                            .setChannelId("ekhanei_high_importance_channel")
                            .setSound("default")
                            .setDefaultSound(true)
                            .setDefaultVibrateTimings(true)
                            .build())
                    .build();

            com.google.firebase.messaging.ApnsConfig apnsConfig = com.google.firebase.messaging.ApnsConfig.builder()
                    .setAps(com.google.firebase.messaging.Aps.builder()
                            .setSound("default")
                            .setContentAvailable(true)
                            .build())
                    .build();

            messageBuilder.setAndroidConfig(androidConfig)
                    .setApnsConfig(apnsConfig);

            String responseId = FirebaseMessaging.getInstance().send(messageBuilder.build());
            log.info("✅ [FCM] Notification successfully sent to {}. Msg ID: {}", fcmToken, responseId);
            return responseId;

        } catch (FirebaseMessagingException e) {
            log.error("⚠️ [FCM] SDK error sending push notification: {}", e.getMessage());
            return null;
        } catch (Exception e) {
            log.error("⚠️ [FCM] Unexpected failure pushing notification: {}", e.getMessage());
            return null;
        }
    }
}

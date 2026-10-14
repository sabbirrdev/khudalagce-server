package com.company.efood.notification.controller;

import com.company.efood.base.BaseResponse;
import com.company.efood.base.BaseUtils;
import com.company.efood.config.CurrentUserContext;
import com.company.efood.notification.entity.NotificationLog;
import com.company.efood.notification.repository.NotificationLogRepo;
import com.company.efood.notification.service.FcmNotificationService;
import com.company.efood.sys.utils.AppUserType;
import com.company.efood.sys.utils.AuthTokenUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static com.company.efood.base.BaseConstants.*;

@Slf4j
@RestController
@RequestMapping({PRIVET_ENDPOINT + "notifications", SYSTEM_ADMIN_END_POINT + "notifications"})
@RequiredArgsConstructor
public class NotificationLogController {

    private final NotificationLogRepo notificationLogRepo;
    private final BaseUtils baseUtils;
    private final AuthTokenUtils authTokenUtils;
    private final FcmNotificationService fcmNotificationService;

    @GetMapping("/logs")
    public ResponseEntity<BaseResponse> getMyNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request
    ) {
        try {
            Long userId = safeGetUserId(request);
            AppUserType userType = CurrentUserContext.getRole();

            Page<NotificationLog> logs;
            if (AppUserType.SYSTEM_ADMIN.equals(userType) || AppUserType.DEVELOPER.equals(userType)) {
                logs = notificationLogRepo.findAllByOrderByEntryDateDesc(PageRequest.of(page, size));
            } else if (userType != null && userId != null) {
                logs = notificationLogRepo.findByAppUserIdOrAppUserTypeBroadcast(userId, userType, PageRequest.of(page, size));
            } else if (userId != null) {
                logs = notificationLogRepo.findByAppUserIdOrderByEntryDateDesc(userId, PageRequest.of(page, size));
            } else {
                // No valid auth context — return empty list rather than 500
                log.warn("[NOTIFICATIONS] /logs called with no valid userId or userType");
                return ResponseEntity.ok(baseUtils.generateSuccessResponse(Page.empty(), PROCESS_COMPLETE, PROCESS_COMPLETE_BN));
            }

            return ResponseEntity.ok(baseUtils.generateSuccessResponse(logs, PROCESS_COMPLETE, PROCESS_COMPLETE_BN));
        } catch (Exception e) {
            log.error("[NOTIFICATIONS] /logs error: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(baseUtils.generateErrorResponse(e));
        }
    }

    @GetMapping("/unread-count")
    public ResponseEntity<BaseResponse> getUnreadCount(HttpServletRequest request) {
        try {
            Long userId = safeGetUserId(request);
            AppUserType userType = CurrentUserContext.getRole();

            long count = 0;
            if (AppUserType.SYSTEM_ADMIN.equals(userType) || AppUserType.DEVELOPER.equals(userType)) {
                count = notificationLogRepo.countByAppUserTypeAndIsReadFalse(AppUserType.SYSTEM_ADMIN);
            } else if (userType != null && userId != null) {
                count = notificationLogRepo.countUnreadForUserOrBroadcast(userId, userType);
            } else if (userId != null) {
                count = notificationLogRepo.countByAppUserIdAndIsReadFalse(userId);
            }

            Map<String, Object> data = new HashMap<>();
            data.put("unreadCount", count);
            return ResponseEntity.ok(baseUtils.generateSuccessResponse(data, PROCESS_COMPLETE, PROCESS_COMPLETE_BN));
        } catch (Exception e) {
            log.error("[NOTIFICATIONS] /unread-count error: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(baseUtils.generateErrorResponse(e));
        }
    }

    @PostMapping("/mark-read/{id}")
    public ResponseEntity<BaseResponse> markRead(@PathVariable Long id) {
        try {
            notificationLogRepo.findById(id).ifPresent(nLog -> {
                nLog.setIsRead(true);
                nLog.setReadAt(LocalDateTime.now());
                notificationLogRepo.save(nLog);
            });
            return ResponseEntity.ok(baseUtils.generateSuccessResponse("Notification marked as read", PROCESS_COMPLETE, PROCESS_COMPLETE_BN));
        } catch (Exception e) {
            log.error("[NOTIFICATIONS] /mark-read error: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(baseUtils.generateErrorResponse(e));
        }
    }

    @PostMapping("/mark-all-read")
    public ResponseEntity<BaseResponse> markAllRead(HttpServletRequest request) {
        try {
            Long userId = safeGetUserId(request);
            AppUserType userType = CurrentUserContext.getRole();

            Page<NotificationLog> logs;
            if (userId != null && userType != null) {
                logs = notificationLogRepo.findByAppUserIdOrAppUserTypeBroadcast(userId, userType, PageRequest.of(0, 200));
            } else if (userId != null) {
                logs = notificationLogRepo.findByAppUserIdOrderByEntryDateDesc(userId, PageRequest.of(0, 200));
            } else {
                return ResponseEntity.ok(baseUtils.generateSuccessResponse("No notifications to mark", PROCESS_COMPLETE, PROCESS_COMPLETE_BN));
            }

            for (NotificationLog nLog : logs.getContent()) {
                if (!Boolean.TRUE.equals(nLog.getIsRead())) {
                    nLog.setIsRead(true);
                    nLog.setReadAt(LocalDateTime.now());
                }
            }
            notificationLogRepo.saveAll(logs.getContent());
            return ResponseEntity.ok(baseUtils.generateSuccessResponse("All notifications marked as read", PROCESS_COMPLETE, PROCESS_COMPLETE_BN));
        } catch (Exception e) {
            log.error("[NOTIFICATIONS] /mark-all-read error: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(baseUtils.generateErrorResponse(e));
        }
    }

    /**
     * Admin-only: Send a broadcast push notification to all customer devices.
     * Body: { "title": "...", "body": "...", "notificationType": "OFFER" }
     */
    @PostMapping("/broadcast-to-customers")
    public ResponseEntity<BaseResponse> broadcastToCustomers(@RequestBody Map<String, String> payload) {
        try {
            AppUserType userType = CurrentUserContext.getRole();
            if (!AppUserType.SYSTEM_ADMIN.equals(userType) && !AppUserType.DEVELOPER.equals(userType)) {
                return ResponseEntity.status(403).body(baseUtils.generateErrorResponse(new RuntimeException("Access denied")));
            }
            String title = payload.getOrDefault("title", "");
            String body = payload.getOrDefault("body", "");
            String notificationType = payload.getOrDefault("notificationType", "ADMIN_BROADCAST");

            if (title.isBlank() || body.isBlank()) {
                return ResponseEntity.badRequest().body(baseUtils.generateErrorResponse(new RuntimeException("title and body are required")));
            }

            fcmNotificationService.sendBroadcastToAllCustomers(title, body, notificationType);
            return ResponseEntity.ok(baseUtils.generateSuccessResponse("Broadcast sent to all customers", PROCESS_COMPLETE, PROCESS_COMPLETE_BN));
        } catch (Exception e) {
            log.error("[NOTIFICATIONS] /broadcast-to-customers error: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(baseUtils.generateErrorResponse(e));
        }
    }

    /**
     * Safely extracts the userId from the JWT in the request.
     * Returns null instead of throwing if the token is absent or invalid.
     */
    private Long safeGetUserId(HttpServletRequest request) {
        try {
            return authTokenUtils.getUserIdFromRequest(request);
        } catch (Exception e) {
            log.debug("[NOTIFICATIONS] Could not extract userId from request: {}", e.getMessage());
            return null;
        }
    }
}

package com.company.efood.support.controller;

import com.company.efood.base.BaseResponse;
import com.company.efood.base.BaseUtils;
import com.company.efood.support.entity.SupportConversation;
import com.company.efood.support.entity.SupportConversationMessage;
import com.company.efood.support.repository.SupportConversationMessageRepo;
import com.company.efood.support.repository.SupportConversationRepo;
import com.company.efood.sys.entity.AppUser;
import com.company.efood.sys.repository.AppUserRepo;
import com.company.efood.sys.utils.AuthTokenUtils;
import com.company.efood.websocket.service.LiveNotificationService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.company.efood.base.BaseConstants.PROCESS_COMPLETE;
import static com.company.efood.base.BaseConstants.PROCESS_COMPLETE_BN;
import static com.company.efood.base.BaseConstants.SYSTEM_ADMIN_END_POINT;

@RestController
@RequestMapping({SYSTEM_ADMIN_END_POINT + "support", "/api/v1/sya/support", "/api/sya/support"})
@AllArgsConstructor
public class AdminSupportChatController {

    private final SupportConversationRepo supportConversationRepo;
    private final SupportConversationMessageRepo supportConversationMessageRepo;
    private final AppUserRepo appUserRepo;
    private final BaseUtils baseUtils;
    private final AuthTokenUtils authTokenUtils;
    private final LiveNotificationService liveNotificationService;

    // ─── Safe DTOs ───────────────────────────────────────────────────────────

    private Map<String, Object> toConversationDto(SupportConversation c) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", c.getId());
        m.put("subject", c.getSubject());
        m.put("status", c.getStatus());
        m.put("lastMessage", c.getLastMessage());
        m.put("entryDate", c.getEntryDate() != null
                ? c.getEntryDate().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null);
        if (c.getCustomer() != null) {
            Map<String, Object> cust = new HashMap<>();
            cust.put("id", c.getCustomer().getId());
            cust.put("displayName", c.getCustomer().getDisplayName());
            cust.put("username", c.getCustomer().getUsername());
            cust.put("phone", c.getCustomer().getPhone());
            m.put("customer", cust);
        }
        return m;
    }

    private Map<String, Object> toMessageDto(SupportConversationMessage msg) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", msg.getId());
        m.put("conversationId", msg.getConversation() != null ? msg.getConversation().getId() : null);
        m.put("senderRole", msg.getSenderRole());
        m.put("messageText", msg.getMessageText());
        m.put("entryDate", msg.getEntryDate() != null
                ? msg.getEntryDate().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null);
        return m;
    }

    // ──────────────────────────────────────────────────────────────────────────

    @GetMapping("/conversations")
    public BaseResponse getAllConversations(@RequestParam(required = false) String status) {
        try {
            List<SupportConversation> list;
            if (status != null && !status.isBlank() && !status.equalsIgnoreCase("ALL")) {
                list = supportConversationRepo.findByStatusOrderByEntryDateDesc(status.toUpperCase());
            } else {
                list = supportConversationRepo.findAllByOrderByEntryDateDesc();
            }

            List<Map<String, Object>> dtos = list.stream()
                    .map(this::toConversationDto)
                    .collect(Collectors.toList());

            return baseUtils.generateSuccessResponse(dtos, PROCESS_COMPLETE, PROCESS_COMPLETE_BN);
        } catch (Exception ex) {
            return baseUtils.generateErrorResponse(ex);
        }
    }

    @GetMapping("/conversations/{conversationId}")
    public BaseResponse getConversationDetails(@PathVariable Long conversationId) {
        try {
            SupportConversation conversation = supportConversationRepo.findById(conversationId)
                    .orElseThrow(() -> new RuntimeException("Conversation not found with id: " + conversationId));

            List<SupportConversationMessage> messages =
                    supportConversationMessageRepo.findByConversationOrderByEntryDateAsc(conversation);

            Map<String, Object> payload = new HashMap<>();
            payload.put("conversation", toConversationDto(conversation));
            payload.put("messages", messages.stream().map(this::toMessageDto).collect(Collectors.toList()));

            return baseUtils.generateSuccessResponse(payload, PROCESS_COMPLETE, PROCESS_COMPLETE_BN);
        } catch (Exception ex) {
            return baseUtils.generateErrorResponse(ex);
        }
    }

    @PostMapping("/conversations/{conversationId}/message")
    public BaseResponse sendAdminMessage(@PathVariable Long conversationId,
                                         @RequestBody Map<String, String> body,
                                         HttpServletRequest request) {
        try {
            String messageText = body.get("message");
            if (messageText == null || messageText.isBlank()) {
                return BaseResponse.builder()
                        .status(false)
                        .statusCode(HttpStatus.BAD_REQUEST.value())
                        .message("Message cannot be empty")
                        .build();
            }

            Long adminUserId = authTokenUtils.getUserIdFromRequest(request);
            SupportConversation conversation = supportConversationRepo.findById(conversationId)
                    .orElseThrow(() -> new RuntimeException("Conversation not found with id: " + conversationId));

            SupportConversationMessage message = new SupportConversationMessage();
            message.setConversation(conversation);
            message.setSenderRole("ADMIN");
            message.setMessageText(messageText);
            message.setEntryUser(adminUserId);
            message.setEntryDate(LocalDateTime.now());
            SupportConversationMessage saved = supportConversationMessageRepo.save(message);

            conversation.setLastMessage(messageText);
            if ("CLOSED".equalsIgnoreCase(conversation.getStatus())) {
                conversation.setStatus("OPEN");
            }
            supportConversationRepo.save(conversation);

            // Broadcast real-time message event via websocket
            liveNotificationService.sendSupportMessage(conversationId, Map.of(
                    "conversationId", conversationId,
                    "senderRole", "ADMIN",
                    "message", messageText
            ));

            return baseUtils.generateSuccessResponse(toMessageDto(saved),
                    "Support reply sent successfully", "সাপোর্ট রিপ্লাই সফলভাবে পাঠানো হয়েছে");
        } catch (Exception ex) {
            return baseUtils.generateErrorResponse(ex);
        }
    }

    @PostMapping("/conversations/{conversationId}/status")
    public BaseResponse updateConversationStatus(@PathVariable Long conversationId,
                                                 @RequestBody Map<String, String> body) {
        try {
            String newStatus = body.get("status");
            if (newStatus == null || newStatus.isBlank()) {
                return BaseResponse.builder()
                        .status(false)
                        .statusCode(HttpStatus.BAD_REQUEST.value())
                        .message("Status is required")
                        .build();
            }

            SupportConversation conversation = supportConversationRepo.findById(conversationId)
                    .orElseThrow(() -> new RuntimeException("Conversation not found with id: " + conversationId));

            conversation.setStatus(newStatus.trim().toUpperCase());
            SupportConversation updated = supportConversationRepo.save(conversation);

            return baseUtils.generateSuccessResponse(toConversationDto(updated),
                    "Conversation status updated to " + conversation.getStatus(),
                    "কথোপকথনের স্ট্যাটাস পরিবর্তিত হয়েছে: " + conversation.getStatus());
        } catch (Exception ex) {
            return baseUtils.generateErrorResponse(ex);
        }
    }
}

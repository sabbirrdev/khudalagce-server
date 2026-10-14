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

import static com.company.efood.base.BaseConstants.CUSTOMER_END_POINT;
import static com.company.efood.base.BaseConstants.PROCESS_COMPLETE;
import static com.company.efood.base.BaseConstants.PROCESS_COMPLETE_BN;

@RestController
@RequestMapping({CUSTOMER_END_POINT + "support", "/api/v1/customer/support", "/api/customer/support"})
@AllArgsConstructor
public class SupportChatController {

    private final SupportConversationRepo supportConversationRepo;
    private final SupportConversationMessageRepo supportConversationMessageRepo;
    private final AppUserRepo appUserRepo;
    private final BaseUtils baseUtils;
    private final AuthTokenUtils authTokenUtils;
    private final LiveNotificationService liveNotificationService;

    // ─── Safe DTOs to avoid Hibernate lazy-load serialization crashes ──────────

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

    @PostMapping("/chat")
    public BaseResponse createConversation(@RequestBody Map<String, String> body, HttpServletRequest request) {
        try {
            String subject = body.getOrDefault("subject", "Customer Live Support");
            String message = body.getOrDefault("message", "");
            Long userId = authTokenUtils.getUserIdFromRequest(request);
            AppUser customer = appUserRepo.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Customer not found"));

            // Professional Single-Thread Support:
            // One user has ONE unified conversation rather than multiple tickets.
            List<SupportConversation> existingList =
                    supportConversationRepo.findByCustomerOrderByEntryDateDesc(customer);

            SupportConversation conversation = existingList.stream()
                    .filter(c -> !"CLOSED".equalsIgnoreCase(c.getStatus()))
                    .findFirst()
                    .orElse(null);

            if (conversation == null && !existingList.isEmpty()) {
                conversation = existingList.get(0);
                conversation.setStatus("OPEN");
            }

            if (conversation == null) {
                conversation = new SupportConversation();
                conversation.setCustomer(customer);
                conversation.setSubject(subject);
                conversation.setStatus("OPEN");
                conversation.setLastMessage(message.isBlank() ? null : message);
                conversation.setEntryUser(userId);
                conversation.setEntryDate(LocalDateTime.now());
                conversation = supportConversationRepo.save(conversation);
            } else {
                if ("CLOSED".equalsIgnoreCase(conversation.getStatus())) {
                    conversation.setStatus("OPEN");
                }
                if (!message.isBlank()) {
                    conversation.setLastMessage(message);
                }
                conversation = supportConversationRepo.save(conversation);
            }

            if (!message.isBlank()) {
                SupportConversationMessage firstMessage = new SupportConversationMessage();
                firstMessage.setConversation(conversation);
                firstMessage.setSenderRole("CUSTOMER");
                firstMessage.setMessageText(message);
                firstMessage.setEntryUser(userId);
                firstMessage.setEntryDate(LocalDateTime.now());
                supportConversationMessageRepo.save(firstMessage);

                liveNotificationService.sendSupportMessage(conversation.getId(), Map.of(
                        "conversationId", conversation.getId(),
                        "senderRole", "CUSTOMER",
                        "message", message
                ));
            }

            return baseUtils.generateSuccessResponse(toConversationDto(conversation),
                    "Support conversation ready", "সাপোর্ট কথোপকথন প্রস্তুত");
        } catch (Exception ex) {
            return baseUtils.generateErrorResponse(ex);
        }
    }

    @GetMapping("/chat/active")
    public BaseResponse getActiveConversation(HttpServletRequest request) {
        try {
            Long userId = authTokenUtils.getUserIdFromRequest(request);
            AppUser customer = appUserRepo.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Customer not found"));

            List<SupportConversation> existingList =
                    supportConversationRepo.findByCustomerOrderByEntryDateDesc(customer);

            SupportConversation conversation = existingList.stream()
                    .filter(c -> !"CLOSED".equalsIgnoreCase(c.getStatus()))
                    .findFirst()
                    .orElse(null);

            if (conversation == null && !existingList.isEmpty()) {
                conversation = existingList.get(0);
            }

            if (conversation == null) {
                conversation = new SupportConversation();
                conversation.setCustomer(customer);
                conversation.setSubject("Customer Live Support");
                conversation.setStatus("OPEN");
                conversation.setEntryUser(userId);
                conversation.setEntryDate(LocalDateTime.now());
                conversation = supportConversationRepo.save(conversation);
            }

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

    @GetMapping("/chat")
    public BaseResponse getMyConversations(HttpServletRequest request) {
        try {
            Long userId = authTokenUtils.getUserIdFromRequest(request);
            AppUser customer = appUserRepo.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Customer not found"));
            List<SupportConversation> conversations =
                    supportConversationRepo.findByCustomerOrderByEntryDateDesc(customer);
            List<Map<String, Object>> dtoList = conversations.stream()
                    .map(this::toConversationDto)
                    .collect(Collectors.toList());
            return baseUtils.generateSuccessResponse(dtoList, PROCESS_COMPLETE, PROCESS_COMPLETE_BN);
        } catch (Exception ex) {
            return baseUtils.generateErrorResponse(ex);
        }
    }

    @GetMapping("/chat/{conversationId}")
    public BaseResponse getConversation(@PathVariable Long conversationId, HttpServletRequest request) {
        try {
            Long userId = authTokenUtils.getUserIdFromRequest(request);
            AppUser customer = appUserRepo.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Customer not found"));
            SupportConversation conversation = supportConversationRepo.findById(conversationId)
                    .orElseThrow(() -> new RuntimeException("Conversation not found"));

            if (!conversation.getCustomer().getId().equals(customer.getId())) {
                throw new RuntimeException("Conversation does not belong to the current customer");
            }

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

    @PostMapping("/chat/{conversationId}/message")
    public BaseResponse sendMessage(@PathVariable Long conversationId,
                                    @RequestBody Map<String, String> body,
                                    HttpServletRequest request) {
        try {
            Long userId = authTokenUtils.getUserIdFromRequest(request);
            String messageText = body.get("message");
            if (messageText == null || messageText.isBlank()) {
                return BaseResponse.builder()
                        .status(false)
                        .statusCode(HttpStatus.BAD_REQUEST.value())
                        .message("Message cannot be empty")
                        .build();
            }

            AppUser customer = appUserRepo.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Customer not found"));
            SupportConversation conversation = supportConversationRepo.findById(conversationId)
                    .orElseThrow(() -> new RuntimeException("Conversation not found"));

            if (!conversation.getCustomer().getId().equals(customer.getId())) {
                throw new RuntimeException("Conversation does not belong to the current customer");
            }

            SupportConversationMessage message = new SupportConversationMessage();
            message.setConversation(conversation);
            message.setSenderRole("CUSTOMER");
            message.setMessageText(messageText);
            message.setEntryUser(userId);
            message.setEntryDate(LocalDateTime.now());
            SupportConversationMessage saved = supportConversationMessageRepo.save(message);

            conversation.setLastMessage(messageText);
            conversation.setStatus("OPEN");
            supportConversationRepo.save(conversation);

            liveNotificationService.sendSupportMessage(conversationId, Map.of(
                    "conversationId", conversationId,
                    "senderRole", "CUSTOMER",
                    "message", messageText
            ));

            return baseUtils.generateSuccessResponse(toMessageDto(saved),
                    "Message sent", "মেসেজ পাঠানো হয়েছে");
        } catch (Exception ex) {
            return baseUtils.generateErrorResponse(ex);
        }
    }
}

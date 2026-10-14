package com.company.efood.support.controller;

import com.company.efood.support.entity.SupportConversation;
import com.company.efood.support.entity.SupportConversationMessage;
import com.company.efood.support.repository.SupportConversationMessageRepo;
import com.company.efood.support.repository.SupportConversationRepo;
import com.company.efood.sys.entity.AppUser;
import com.company.efood.sys.repository.AppUserRepo;
import com.company.efood.websocket.service.LiveNotificationService;
import lombok.AllArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;
import java.util.Map;

@Controller
@AllArgsConstructor
public class SupportSocketController {

    private final SupportConversationRepo supportConversationRepo;
    private final SupportConversationMessageRepo supportConversationMessageRepo;
    private final AppUserRepo appUserRepo;
    private final LiveNotificationService liveNotificationService;

    @MessageMapping("/support/{conversationId}")
    public void handleSupportSocketMessage(@DestinationVariable Long conversationId,
                                            @Payload Map<String, String> payload) {
        String messageText = payload.get("message");
        String senderRole = payload.getOrDefault("senderRole", "CUSTOMER");
        Long senderUserId = payload.containsKey("senderUserId") ? Long.parseLong(payload.get("senderUserId")) : null;

        if (messageText == null || messageText.isBlank()) {
            return;
        }

        SupportConversation conversation = supportConversationRepo.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found"));

        AppUser sender = senderUserId != null ? appUserRepo.findById(senderUserId).orElse(null) : null;
        if (sender == null) {
            throw new IllegalArgumentException("Sender user not found");
        }
        boolean isOwner = conversation.getCustomer().getId().equals(sender.getId());
        boolean isAdmin = sender.getAppUserType() != null &&
                (sender.getAppUserType().name().contains("ADMIN") || sender.getAppUserType().name().contains("DEV"));
        if (!isOwner && !isAdmin) {
            throw new IllegalArgumentException("Only the conversation owner or support admin can send messages over socket");
        }

        String resolvedRole = isAdmin ? "ADMIN" : senderRole.toUpperCase();

        SupportConversationMessage message = new SupportConversationMessage();
        message.setConversation(conversation);
        message.setSenderRole(resolvedRole);
        message.setMessageText(messageText);
        message.setEntryUser(sender.getId());
        message.setEntryDate(LocalDateTime.now());
        supportConversationMessageRepo.save(message);

        conversation.setLastMessage(messageText);
        conversation.setStatus("OPEN");
        supportConversationRepo.save(conversation);

        liveNotificationService.sendSupportMessage(conversationId, Map.of(
                "conversationId", conversationId,
                "senderRole", senderRole,
                "message", messageText
        ));
    }
}

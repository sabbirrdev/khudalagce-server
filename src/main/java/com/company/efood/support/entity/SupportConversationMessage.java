package com.company.efood.support.entity;

import com.company.efood.base.BaseEntity;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@Table(name = "SUPPORT_CONVERSATION_MESSAGE")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class SupportConversationMessage extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CONVERSATION_ID", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "customer", "messages"})
    private SupportConversation conversation;

    @Column(name = "SENDER_ROLE", nullable = false)
    private String senderRole;

    @Column(name = "MESSAGE_TEXT", nullable = false, columnDefinition = "TEXT")
    private String messageText;
}


package com.company.efood.support.entity;

import com.company.efood.base.BaseEntity;
import com.company.efood.sys.entity.AppUser;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@Table(name = "SUPPORT_CONVERSATION")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class SupportConversation extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CUSTOMER_ID", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "customer", "seller",
            "raider", "employee", "passwordPolicy", "roles", "authorities",
            "accountExpired", "credentialsExpired", "accountLocked", "phoneVerified"})
    private AppUser customer;

    @Column(name = "SUBJECT")
    private String subject;

    @Column(name = "STATUS")
    private String status = "OPEN";

    @Column(name = "LAST_MESSAGE")
    private String lastMessage;
}


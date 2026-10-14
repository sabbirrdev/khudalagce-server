package com.company.efood.sys.entity;

import com.company.efood.base.BaseEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "PAYOUT_ACCOUNT", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"USER_ID", "USER_TYPE"})
})
@Data
@EqualsAndHashCode(callSuper = true)
public class PayoutAccount extends BaseEntity {

    @Column(name = "USER_ID", nullable = false)
    private Long userId;

    @Column(name = "USER_TYPE", nullable = false)
    private String userType; // "SELLER" or "RAIDER"

    @Column(name = "PAYOUT_METHOD", nullable = false)
    private String payoutMethod; // "BANK" or "MFS"

    // Bank Details
    @Column(name = "BANK_NAME")
    private String bankName;

    @Column(name = "ACCOUNT_HOLDER_NAME")
    private String accountHolderName;

    @Column(name = "ACCOUNT_NUMBER")
    private String accountNumber;

    @Column(name = "ROUTING_NUMBER")
    private String routingNumber;

    @Column(name = "BRANCH_NAME")
    private String branchName;

    // Mobile Financial Services (bKash, Nagad, Rocket)
    @Column(name = "MFS_PROVIDER")
    private String mfsProvider;

    @Column(name = "MFS_NUMBER")
    private String mfsNumber;

    @Column(name = "MFS_ACCOUNT_TYPE")
    private String mfsAccountType; // "Personal", "Agent", "Merchant"

    @Column(name = "IS_VERIFIED", columnDefinition = "boolean default false")
    private Boolean isVerified = false;
}

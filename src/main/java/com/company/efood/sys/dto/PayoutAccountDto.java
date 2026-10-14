package com.company.efood.sys.dto;

import com.company.efood.base.BaseDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class PayoutAccountDto extends BaseDto {
    private Long userId;
    private String userType; // "SELLER" or "RAIDER"
    private String payoutMethod; // "BANK" or "MFS"

    // Bank Details
    private String bankName;
    private String accountHolderName;
    private String accountNumber;
    private String routingNumber;
    private String branchName;

    // Mobile Financial Services (bKash, Nagad, Rocket)
    private String mfsProvider;
    private String mfsNumber;
    private String mfsAccountType;
    private Boolean isVerified = false;

    // Backward-compatible aliases for mobile apps and frontend
    public String getChannelType() {
        if ("BANK".equalsIgnoreCase(payoutMethod)) return "BANK";
        return mfsProvider != null ? mfsProvider.toUpperCase() : "BKASH";
    }

    public void setChannelType(String channelType) {
        if (channelType != null) {
            String norm = channelType.trim().toUpperCase();
            if ("BANK".equalsIgnoreCase(norm)) {
                this.payoutMethod = "BANK";
            } else {
                this.payoutMethod = "MFS";
                this.mfsProvider = channelType;
            }
        }
    }

    public String getAccountName() {
        return accountHolderName;
    }

    public void setAccountName(String accountName) {
        this.accountHolderName = accountName;
    }

    public Boolean getIsPrimary() {
        return Boolean.TRUE;
    }

    public void setIsPrimary(Boolean isPrimary) {
        // no-op
    }
}

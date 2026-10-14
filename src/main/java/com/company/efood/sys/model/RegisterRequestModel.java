package com.company.efood.sys.model;

import lombok.Data;

@Data
public class RegisterRequestModel {
    String displayName;
    String username;
    String password;
    String phone;
    String gender;
    String vehicleType;
    String vehicleNumber;
    String referralCode;
    Long shopCategoryId;
    String shopCategory;
    java.util.Map<String, String> complianceDocuments;
}


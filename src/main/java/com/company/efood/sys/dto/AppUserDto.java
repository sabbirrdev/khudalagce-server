package com.company.efood.sys.dto;

import com.company.efood.base.BaseDto;
import com.company.efood.sys.utils.AppUserType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * @version 1.0.0
 * @Author Md. Sabbir Hossain
 * @Email sabbirr883@gmail.com
 * @Since March 1, 2024
 */

@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
public class AppUserDto extends BaseDto implements Serializable {

    private static final long serialVersionUID = 1L;
    private Long passwordPolicyId;
    private String passwordPolicyName;
    private String username;
    private String password;
    private Long userTypeId;
    private AppUserType appUserType;
    private String otp;
    private String phone;
    private String displayName;
    private Boolean phoneVerified;

}

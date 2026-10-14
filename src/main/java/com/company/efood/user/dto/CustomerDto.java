package com.company.efood.user.dto;

import com.company.efood.base.BaseDto;
import com.company.efood.sys.dto.AddressDto;
import com.company.efood.sys.dto.AppUserDto;
import com.company.efood.sys.utils.Gender;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class CustomerDto extends BaseDto {
    private AppUserDto appUser;
    private String fullName;
    private String phone;
    private String nid;
    private String nidFront;
    private String nidBack;
    private String otherDoc;
    private Date birthDate;
    private Gender gender;
    private AddressDto address;
}

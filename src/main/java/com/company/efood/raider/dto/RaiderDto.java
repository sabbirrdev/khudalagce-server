package com.company.efood.raider.dto;

import com.company.efood.base.BaseDto;
import com.company.efood.sys.dto.AddressDto;
import com.company.efood.sys.dto.AppUserDto;
import com.company.efood.sys.entity.Address;
import com.company.efood.sys.entity.Shop;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.util.Date;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
public class RaiderDto extends BaseDto {
    private String fullName;
    private String phone;
    private String nid;
    private String nidFront;
    private String nidBack;
    private String otherDoc;
    private Date birthDate;
    private AppUserDto appUser;
    private AddressDto address;
    private String vehicleType;
    private String vehicleNumber;
    private Boolean isAvailable = true;
    private java.util.Map<String, String> complianceDocuments;

}

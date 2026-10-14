package com.company.efood.seller.dto;

import com.company.efood.base.BaseDto;
import com.company.efood.sys.dto.AddressDto;
import com.company.efood.sys.entity.Address;
import com.company.efood.sys.entity.Shop;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
public class SellerDto extends BaseDto {

    private String fullName;
    private String phone;
    private String nid;
    private String nidFront;
    private String nidBack;
    private String otherDoc;
    private LocalDate birthDate;
    private AddressDto address;
    private Long shopCategoryId;
    private String shopCategoryName;
    private Long serviceId;
    private String serviceName;
}

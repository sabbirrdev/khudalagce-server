package com.company.efood.sys.dto;

import com.company.efood.base.BaseDto;
import com.company.efood.sys.entity.Address;
import com.company.efood.sys.entity.Product;
import com.company.efood.sys.entity.Review;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
public class ShopDto extends BaseDto {
    private String shopName;
    private String phoneNumber;
    private String shopNameBn;
    private String about;
    private String logoUrl;
    private Long sellerId;
    private Long commissionPolicyId;
    private Long zoneId;
    private Double shopLat;
    private Double shopLon;
    private String shopAddress;
    private String shopType;
}

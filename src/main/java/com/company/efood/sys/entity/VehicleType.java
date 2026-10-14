package com.company.efood.sys.entity;

import com.company.efood.base.BaseEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "VEHICLE_TYPE")
@Data
@EqualsAndHashCode(callSuper = true)
public class VehicleType extends BaseEntity {

    @Column(name = "NAME", nullable = false, unique = true)
    private String name;

    @Column(name = "CODE", nullable = false, unique = true)
    private String code;

    @Column(name = "DESCRIPTION", columnDefinition = "text")
    private String description;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "VEHICLE_TYPE_DOCUMENTS", joinColumns = @JoinColumn(name = "VEHICLE_TYPE_ID"))
    @Column(name = "DOCUMENT_NAME")
    private List<String> requiredDocuments = new ArrayList<>();
}

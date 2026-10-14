package com.company.efood.zone.entity;

import com.company.efood.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "SYA_ZONE")
public class Zone extends BaseEntity {

    @Column(name = "NAME", nullable = false)
    private String name;

    @Column(name = "NAME_BN")
    private String nameBn;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "UPAZILA_ID", nullable = false)
    private Upazila upazila;

    /** Base delivery fee (flat charge regardless of distance/weight) */
    @Column(name = "BASE_DELIVERY_FEE", nullable = false)
    private BigDecimal baseDeliveryFee = BigDecimal.ZERO;

    /** Additional fee per km after the free threshold (default 0) */
    @Column(name = "PER_KM_CHARGE", nullable = false)
    private BigDecimal perKmCharge = BigDecimal.ZERO;

    /** Additional fee per kg of product weight above freeWeightGrams. Default 0 = no weight surcharge */
    @Column(name = "PER_KG_CHARGE")
    private BigDecimal perKgCharge = BigDecimal.ZERO;

    /** Weight (grams) included free in the base fee. Surcharge starts above this. Default 1000g (1 kg) */
    @Column(name = "FREE_WEIGHT_GRAMS")
    private Integer freeWeightGrams = 1000;

    /** Minimum delivery fee (floor). If calculated fee < minFee, use minFee. Default = baseDeliveryFee */
    @Column(name = "MIN_DELIVERY_FEE")
    private BigDecimal minDeliveryFee;

    /** Free distance threshold in km. Distance surcharge applies beyond this. Default 1 km */
    @Column(name = "FREE_DISTANCE_KM")
    private Double freeDistanceKm = 1.0;

    /** Hub / dispatch center latitude (zone-level origin; fallback when branch GPS missing) */
    @Column(name = "HUB_LAT")
    private Double hubLat;

    /** Hub / dispatch center longitude */
    @Column(name = "HUB_LON")
    private Double hubLon;

    @Column(name = "GEOFENCE_POLYGON", columnDefinition = "text")
    private String geoFencePolygon;

    @Column(name = "IS_DELIVERABLE", columnDefinition = "boolean default true")
    private Boolean isDeliverable = true;
}


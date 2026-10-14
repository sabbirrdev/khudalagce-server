package com.company.efood.raider.entity;

import com.company.efood.base.BaseEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@Table(name = "RAIDER_EARNING")
public class RaiderEarning extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "RAIDER_ID", nullable = false)
    private Raider raider;

    @Column(name = "ORDER_ID")
    private Long orderId;

    @Column(name = "EARNED_AMOUNT")
    private Double earnedAmount;

    @Column(name = "PAID")
    private Boolean paid = false;

    @Column(name = "EARNED_AT")
    private LocalDateTime earnedAt = LocalDateTime.now();
}

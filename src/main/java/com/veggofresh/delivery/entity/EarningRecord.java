package com.veggofresh.delivery.entity;

import com.veggofresh.platform.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Where;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "delivery_earnings")
@Getter
@Setter
@Where(clause = "deleted_at IS NULL")
public class EarningRecord extends BaseEntity {

    @Column(name = "delivery_partner_user_id", nullable = false)
    private UUID deliveryPartnerUserId;

    @Column(name = "order_id", nullable = false, unique = true)
    private UUID orderId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;
}

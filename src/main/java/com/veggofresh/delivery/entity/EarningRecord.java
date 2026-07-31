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

    // ── Fare breakdown (Earnings phase) ─────────────────────────────────
    // amount above = basePay + distanceFare + peakBonus + tip (kept as the running
    // total for backward compatibility with anything already summing on it).
    // peakBonus and tip are ALWAYS 0 right now -- no surge/demand system exists,
    // and no tip-collection mechanism exists anywhere (Customer/Payment don't
    // capture tips at checkout). Structurally present so the response shape
    // matches the mockup; will populate once those systems exist. See NOTES.md.
    @Column(name = "base_pay", nullable = false, precision = 12, scale = 2)
    private BigDecimal basePay = BigDecimal.ZERO;

    @Column(name = "distance_fare", nullable = false, precision = 12, scale = 2)
    private BigDecimal distanceFare = BigDecimal.ZERO;

    @Column(name = "peak_bonus", nullable = false, precision = 12, scale = 2)
    private BigDecimal peakBonus = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal tip = BigDecimal.ZERO;
}

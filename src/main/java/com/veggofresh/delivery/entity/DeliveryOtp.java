package com.veggofresh.delivery.entity;

import com.veggofresh.platform.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Where;

import java.time.Instant;
import java.util.UUID;

/**
 * Locally-owned OTPs -- both pickup (NEW this round) and drop (pre-existing).
 *
 * WORKAROUND NOTE (pre-existing, still true): the Delivery module spec calls for
 * CustomerOrderService.getDeliveryOtp(orderId), but per PROJECT_STATE that method is
 * legacy/weak and should NOT be used -- Delivery keeps its own real OTP system here.
 *
 * SCHEMA CHANGE this round: added `type` (PICKUP/DROP). Unique constraint moved from
 * assignmentId alone to (assignmentId, type) composite -- one assignment now legitimately
 * has up to two OTP rows. otpCode widened in practice to 6 digits (both types) -- the
 * column itself was already length 10, so this needed no schema change, only a
 * generation-code change in DeliveryAssignmentServiceImpl.
 */
@Entity
@Table(name = "delivery_otps")
@Getter
@Setter
@Where(clause = "deleted_at IS NULL")
public class DeliveryOtp extends BaseEntity {

    @Column(name = "assignment_id", nullable = false)
    private UUID assignmentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private DeliveryOtpType type;

    @Column(name = "otp_code", nullable = false, length = 10)
    private String otpCode;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private boolean verified = false;

    @Column(nullable = false)
    private int attempts = 0;
}

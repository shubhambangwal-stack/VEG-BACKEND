package com.veggofresh.delivery.entity;

import com.veggofresh.platform.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Where;

import java.time.Instant;
import java.util.UUID;

/**
 * Locally-owned delivery-completion OTP.
 *
 * WORKAROUND NOTE: the Delivery module spec calls for
 * CustomerOrderService.getDeliveryOtp(orderId), but that method does not exist
 * on the current CustomerOrderService stub. Rather than block on it, Delivery
 * generates and verifies its own completion OTP here, scoped to the
 * DeliveryAssignment. Revisit / consolidate once Customer module exposes a
 * real getDeliveryOtp(orderId) — see NOTES.md.
 */
@Entity
@Table(name = "delivery_otps")
@Getter
@Setter
@Where(clause = "deleted_at IS NULL")
public class DeliveryOtp extends BaseEntity {

    @Column(name = "assignment_id", nullable = false, unique = true)
    private UUID assignmentId;

    @Column(name = "otp_code", nullable = false, length = 10)
    private String otpCode;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private boolean verified = false;

    @Column(nullable = false)
    private int attempts = 0;
}

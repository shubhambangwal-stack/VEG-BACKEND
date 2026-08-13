package com.veggofresh.payment.entity;

import com.veggofresh.platform.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Where;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Represents a single Razorpay payment attempt for a VegGoFresh order.
 *
 * <p>One VegGoFresh order can have multiple Payment rows (e.g., if a customer
 * retries after a failure), but only ONE row should ever be in {@code CAPTURED}
 * state per order. The service layer enforces this.
 *
 * <p>Lifecycle:
 * <pre>
 *   createPaymentOrder() → Payment(CREATED, razorpayOrderId set)
 *   verifyPayment()      → Payment(CAPTURED, razorpayPaymentId + signature set)
 *   webhook failure      → Payment(FAILED, failureReason set)
 * </pre>
 */
@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
@Where(clause = "deleted_at IS NULL")
public class Payment extends BaseEntity {

    /** VegGoFresh {@code orders.id} — not a DB FK to avoid cross-module constraint. */
    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    /** ID of the customer who initiated this payment. */
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    // ── Razorpay Identifiers ─────────────────────────────────

    /**
     * Razorpay order ID (format: {@code order_XXXX}).
     * Created via Razorpay API {@code Orders.create()}.
     * Unique per attempt — used as the idempotency key.
     */
    @Column(name = "razorpay_order_id", nullable = false, unique = true, length = 100)
    private String razorpayOrderId;

    /**
     * Razorpay payment ID (format: {@code pay_XXXX}).
     * Populated only after a successful payment capture.
     */
    @Column(name = "razorpay_payment_id", length = 100)
    private String razorpayPaymentId;

    /**
     * HMAC-SHA256 signature returned by Razorpay on successful payment.
     * Stored for audit trail — verified before setting status to CAPTURED.
     */
    @Column(name = "razorpay_signature", length = 500)
    private String razorpaySignature;

    // ── Financial ────────────────────────────────────────────

    /** Amount in INR (human-readable). Converted to paise (×100) when calling Razorpay API. */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 10)
    private String currency = "INR";

    // ── Status ───────────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PaymentStatus status;

    // ── Failure metadata ─────────────────────────────────────

    /**
     * Human-readable failure reason from Razorpay (e.g., "Payment failed due to insufficient funds").
     * Populated only when {@code status = FAILED}.
     */
    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    /**
     * Last webhook event type processed (e.g., {@code payment.captured}, {@code payment.failed}).
     * Used for idempotency audit — webhook handler updates this on each event.
     */
    @Column(name = "webhook_event", length = 100)
    private String webhookEvent;
}

package com.veggofresh.payment.entity;

/**
 * Lifecycle states of a Razorpay payment attempt.
 *
 * <ul>
 *   <li>{@code CREATED}  — Razorpay order created; customer has NOT paid yet.</li>
 *   <li>{@code CAPTURED} — Payment successfully captured by Razorpay; confirmed via
 *                          signature verification or webhook {@code payment.captured}.</li>
 *   <li>{@code FAILED}   — Payment failed or was declined; customer may retry.</li>
 *   <li>{@code REFUNDED} — Payment was refunded (future use — refund flow not in this phase).</li>
 * </ul>
 */
public enum PaymentStatus {

    /** Razorpay order created; awaiting customer payment. */
    CREATED,

    /** Payment successfully captured; VegGoFresh order is now PLACED. */
    CAPTURED,

    /** Payment failed or declined; VegGoFresh order remains PAYMENT_PENDING. */
    FAILED,

    /** Payment refunded. VegGoFresh order should be CANCELLED. Refund flow is future work. */
    REFUNDED
}

package com.veggofresh.payment.entity;

/**
 * Lifecycle of a {@link PayoutRequest}.
 *
 * PENDING     Submitted by vendor/delivery, waiting for admin review.
 * APPROVED    Admin approved -- if payoutsEnabled=true, Razorpay Payout was initiated.
 *             If payoutsEnabled=false, a manual bank transfer is expected.
 * REJECTED    Admin rejected, wallet credit reversed back.
 * COMPLETED   Bank transfer confirmed (Razorpay callback or admin manual confirm).
 * FAILED      Bank transfer failed; wallet credit will be reversed.
 */
public enum PayoutRequestStatus {
    PENDING,
    APPROVED,
    PROCESSING,
    REJECTED,
    COMPLETED,
    FAILED
}

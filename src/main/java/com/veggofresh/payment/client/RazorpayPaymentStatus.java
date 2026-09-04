package com.veggofresh.payment.client;

import java.math.BigDecimal;

/**
 * Minimal projection of a Razorpay Payment object -- just what
 * {@code PaymentServiceImpl} needs to reconcile state (webhook processing,
 * or the fallback poll if a webhook never arrives). Not a full mirror of
 * Razorpay's payment JSON.
 */
public record RazorpayPaymentStatus(
        String razorpayPaymentId,
        String razorpayOrderId,
        /** Razorpay's raw status string: created, authorized, captured, refunded, failed. */
        String status,
        /** Rupees, already converted down from Razorpay's paise. */
        BigDecimal amount,
        String currency
) {
    public boolean isAuthorized() {
        return "authorized".equals(status) || "captured".equals(status);
    }

    public boolean isCaptured() {
        return "captured".equals(status);
    }

    public boolean isFailed() {
        return "failed".equals(status);
    }
}

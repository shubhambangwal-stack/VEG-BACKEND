package com.veggofresh.payment.service;

/**
 * Kept deliberately small this round -- only the reasons real callers actually produce
 * right now. Extend as real Payment/Razorpay integration, vendor/delivery payouts, and
 * the full checkout-hold model get built; don't pre-guess reasons nothing calls yet.
 */
public enum WalletTransactionReason {
    /** Order cancelled (by the customer, or -- once built -- by a re-broadcast limit hit) -- order total refunded to the customer's wallet. */
    ORDER_CANCELLED_REFUND,

    /** Manual balance correction by an operator/admin -- not exposed via any API yet, reserved for direct DB/ops use. */
    MANUAL_ADJUSTMENT
}

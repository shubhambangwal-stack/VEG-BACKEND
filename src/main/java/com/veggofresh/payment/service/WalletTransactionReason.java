package com.veggofresh.payment.service;

/**
 * Kept deliberately small this round -- only the reasons real callers actually produce
 * right now. Extend as real Payment/Razorpay integration, vendor/delivery payouts, and
 * the full checkout-hold model get built; don't pre-guess reasons nothing calls yet.
 */
public enum WalletTransactionReason {
    /** Order cancelled (by the customer, or -- once built -- by a re-broadcast limit hit) -- order total refunded to the customer's wallet. */
    ORDER_CANCELLED_REFUND,
    ORDER_CANCELLED_POST_CAPTURE_REFUND,
    ORDER_VENDOR_SETTLEMENT,
    ORDER_DELIVERY_SETTLEMENT,
    ORDER_PLATFORM_COMMISSION,
    WALLET_TOP_UP,
    PAYOUT_DEBIT,
    PAYOUT_REVERSAL,
    PAYOUT_REQUEST_DEBIT,
    PAYOUT_REJECTED_REFUND,
    PAYOUT_FAILED_REFUND,

    /** Manual balance correction by an operator/admin -- not exposed via any API yet, reserved for direct DB/ops use. */
    MANUAL_ADJUSTMENT
}

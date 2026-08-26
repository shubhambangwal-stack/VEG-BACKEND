package com.veggofresh.payment.entity;

/**
 * Lifecycle of one {@link PaymentOrder} -- i.e. one Razorpay order/payment,
 * which may cover several {@link PaymentOrderLine}s (one checkout, N orders).
 *
 * <pre>
 * CREATED             Razorpay order created (payment_capture:0), customer has not
 *                      yet completed checkout on Razorpay's side.
 * AUTHORIZED          Razorpay confirms the payment was authorized (via verify or
 *                      webhook) -- money is held by Razorpay, NOT captured yet.
 * PARTIALLY_CAPTURED  All lines resolved; at least one ACCEPTED and at least one
 *                      VOIDED. The ACCEPTED lines' sum was captured; the rest was
 *                      never charged (Razorpay auto-releases the remainder).
 * CAPTURED            All lines resolved ACCEPTED; full total_amount captured.
 * VOIDED              All lines resolved VOIDED (every order in the batch was
 *                      rejected/cancelled before any vendor accepted) -- nothing
 *                      was ever captured, no refund needed.
 * FAILED              Razorpay reported the payment failed/was never authorized.
 * </pre>
 */
public enum PaymentOrderStatus {
    CREATED,
    AUTHORIZED,
    PARTIALLY_CAPTURED,
    CAPTURED,
    VOIDED,
    FAILED;

    public boolean isTerminal() {
        return this == PARTIALLY_CAPTURED || this == CAPTURED || this == VOIDED || this == FAILED;
    }
}

package com.veggofresh.payment.entity;

/**
 * Lifecycle states of a Razorpay payment attempt, aligned with the VegGo Fresh authorize/hold model.
 *
 * <ul>
 *   <li>{@code CREATED}    — Razorpay order created; customer has NOT paid yet.</li>
 *   <li>{@code AUTHORIZED} — Customer paid; funds are authorized (held) but NOT captured. Awaiting Vendor accept.</li>
 *   <li>{@code CAPTURED}   — Payment successfully captured by Razorpay upon Vendor accept.</li>
 *   <li>{@code FAILED}     — Payment failed or was declined by the bank.</li>
 *   <li>{@code VOIDED}     — Vendor timeout expired; the authorized hold was released (Voided).</li>
 *   <li>{@code REFUNDED}   — Payment was refunded. Under the new model, post-capture failures result in a full refund to the internal Wallet.</li>
 * </ul>
 */
public enum PaymentStatus {

    /** Razorpay order created; awaiting customer payment. */
    CREATED,

    /** Customer paid; funds are authorized (held) but NOT captured. Awaiting Vendor accept. */
    AUTHORIZED,

    /** Vendor accepted; payment successfully captured (funds actually moved). */
    CAPTURED,

    /** Customer payment attempt failed or was declined by the bank. */
    FAILED,

    /** Vendor timeout expired with no accept; the authorized hold was released (Voided). */
    VOIDED,

    /** 
     * Payment was refunded. Under the new model, post-capture failures 
     * result in a full refund to the internal Wallet.
     */
    REFUNDED
}

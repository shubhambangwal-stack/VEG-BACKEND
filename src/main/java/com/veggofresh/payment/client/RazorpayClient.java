package com.veggofresh.payment.client;

import java.math.BigDecimal;

/**
 * The only place in the module that talks to Razorpay's HTTP API directly.
 * {@code PaymentServiceImpl} depends on this interface, never on
 * {@link RazorpayClientImpl} or a raw {@code RestTemplate} -- keeps
 * Razorpay-specific JSON shapes and error codes out of the orchestrator.
 */
public interface RazorpayClient {

    /**
     * Creates a Razorpay order with {@code payment_capture: 0} (manual
     * capture) -- money is only authorized when the customer pays on
     * Razorpay's Checkout.js widget, never captured until
     * {@link #capturePayment} is called explicitly.
     *
     * @param amount        rupees; converted to paise internally
     * @param currency      e.g. "INR"
     * @param receiptId     our own {@code PaymentOrder.id} as a string, for cross-referencing in the Razorpay dashboard
     * @return Razorpay's order id, e.g. {@code order_ABC123}
     */
    String createOrder(BigDecimal amount, String currency, String receiptId);

    /**
     * Verifies the HMAC-SHA256 signature Razorpay's Checkout.js returns on
     * successful payment: {@code HMAC(razorpayOrderId + "|" + razorpayPaymentId, keySecret)}.
     * Returns false rather than throwing -- the caller decides what a bad
     * signature means (reject the verify call, do NOT touch payment state).
     */
    boolean verifyPaymentSignature(String razorpayOrderId, String razorpayPaymentId, String razorpaySignature);

    /**
     * Verifies an inbound webhook's {@code X-Razorpay-Signature} header:
     * {@code HMAC(rawRequestBody, webhookSecret)}. Must be checked against
     * the RAW body bytes, not a re-serialized object -- any re-serialization
     * can change field order/whitespace and silently break the signature.
     */
    boolean verifyWebhookSignature(String rawPayload, String razorpaySignatureHeader);

    /**
     * Fetches a payment's current status directly from Razorpay -- used by
     * the verify endpoint as a second, authoritative check (never trust the
     * frontend's own claim of success), and by the webhook handler when it
     * needs to double check payment amount before authorizing.
     */
    RazorpayPaymentStatus fetchPaymentStatus(String razorpayPaymentId);

    /**
     * Captures a previously-authorized payment for exactly {@code amount}
     * rupees, which may be less than the full authorized amount (partial
     * capture -- see PaymentOrderLine's status doc for when that happens).
     * Razorpay allows exactly ONE capture call per payment; calling this
     * twice on the same payment is a caller bug, not something this client
     * retries around.
     */
    RazorpayPaymentStatus capturePayment(String razorpayPaymentId, BigDecimal amount, String currency);
    /**
     * Creates a payout via RazorpayX (requires payoutsEnabled=true in config and active KYC).
     *
     * @param fundAccountId The Razorpay Fund Account ID (e.g. "fa_00000000000001")
     * @param amount        The amount to pay out in rupees
     * @param currency      e.g. "INR"
     * @param referenceId   Our internal payout request ID (for cross-referencing)
     * @return The Razorpay Payout ID (e.g. "pout_00000000000001")
     */
    String createPayout(String fundAccountId, BigDecimal amount, String currency, String referenceId);
}

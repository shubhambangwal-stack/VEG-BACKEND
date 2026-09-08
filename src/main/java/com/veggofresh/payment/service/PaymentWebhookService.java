package com.veggofresh.payment.service;

/**
 * Handles inbound Razorpay webhook events. The controller passes the raw request
 * body bytes (as a String) and the {@code X-Razorpay-Signature} header value.
 * Signature verification and idempotency deduplication happen inside this service.
 */
public interface PaymentWebhookService {

    /**
     * Processes an inbound Razorpay webhook event.
     *
     * @param rawPayload           raw JSON body string (must NOT be re-serialized or
     *                             parsed-then-re-serialized before passing here --
     *                             HMAC is computed over the exact bytes received)
     * @param razorpaySignatureHeader value of the {@code X-Razorpay-Signature} header
     */
    void handleWebhook(String rawPayload, String razorpaySignatureHeader);
}

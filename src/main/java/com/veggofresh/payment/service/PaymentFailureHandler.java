package com.veggofresh.payment.service;

import java.util.List;
import java.util.UUID;

/**
 * Lets the payment module tell the order side that a payment it was holding
 * against specific orders has failed.
 *
 * <h2>Why this is an interface and not a direct call</h2>
 *
 * <p>One checkout creates one payment hold spread across N orders, one per cart.
 * When that hold fails, all N orders have to be dealt with or the two sides
 * disagree about reality: the payment is dead while the orders sit in PLACED,
 * which is a state vendors can accept and delivery can complete, so payouts go
 * out for orders nobody paid for.
 *
 * <p>The payment module has no dependency on the customer module, and adding one
 * would invert the existing direction (customer already depends on payment).
 * Rather than introduce a framework event bus that this codebase does not
 * otherwise use, the payment module declares the port it needs and the customer
 * module implements it. The dependency stays one-way: customer -> payment.
 *
 * <p>Implementations MUST be idempotent. Razorpay retries webhooks and the
 * payment module deduplicates by event id, but a delivery failure partway
 * through the batch is also retried, so the same payment order can arrive here
 * more than once.
 */
public interface PaymentFailureHandler {

    /**
     * Reconciles the orders covered by a payment that has failed or been voided.
     *
     * <p>Called after the payment module has already moved its own state, so the
     * implementation only has to deal with the order side: cancel the orders
     * that can no longer be paid for, and tell the customer.
     *
     * @param paymentOrderId the batch that failed
     * @param userId         the customer the batch belongs to
     * @param orderIds       the orders this batch was covering; possibly empty
     *                       for a top-up, which covers no orders at all
     */
    void onPaymentFailed(UUID paymentOrderId, UUID userId, List<UUID> orderIds);
}

package com.veggofresh.payment.entity;

/**
 * Per-Order outcome within one {@link PaymentOrder} batch.
 *
 * PENDING              Vendor has not yet accepted or rejected this Order.
 * ACCEPTED             Vendor accepted (CustomerOrderServiceImpl.acceptOrder()) --
 *                      this line's amount is included in the batch's eventual capture.
 * VOIDED               Order was rejected, or cancelled, before its batch was captured --
 *                      excluded from capture; the customer was never charged for it,
 *                      so there is nothing to refund.
 * CANCELLED_REFUNDED   Order was cancelled AFTER its batch was already captured --
 *                      PaymentServiceImpl.onOrderCancelled() credited the customer
 *                      their wallet here, and this status records that the money has
 *                      already gone back so the line is not refunded twice and is not
 *                      mistaken for still-pending.
 *
 * The terminal statuses above are what make cancellation idempotent: the second
 * call finds the line no longer ACCEPTED/PENDING and does nothing. The order
 * layer must therefore NOT also credit the wallet directly -- the payment module
 * is the only party that knows whether money was actually captured.
 */
public enum PaymentOrderLineStatus {
    PENDING,
    ACCEPTED,
    VOIDED,
    CANCELLED_REFUNDED
}

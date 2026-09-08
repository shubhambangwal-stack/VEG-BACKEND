package com.veggofresh.payment.entity;

/**
 * Per-Order outcome within one {@link PaymentOrder} batch.
 *
 * PENDING              Vendor has not yet accepted or rejected this Order.
 * ACCEPTED             Vendor accepted (CustomerOrderServiceImpl.acceptOrder()) --
 *                      this line's amount is included in the batch's eventual capture.
 * VOIDED               Order was rejected, or cancelled, before its batch was captured --
 *                      excluded from capture; the customer was never charged for it.
 * CANCELLED_REFUNDED   Order was cancelled AFTER its batch was already captured --
 *                      the existing WalletService.credit(ORDER_CANCELLED_REFUND) call
 *                      in OrderServiceImpl.cancelOrder() / CustomerOrderServiceImpl
 *                      .cancelOrderSystemInitiated() already puts the money back in the
 *                      customer's wallet; this status just marks the line so it isn't
 *                      double-refunded and isn't mistaken for still-pending.
 */
public enum PaymentOrderLineStatus {
    PENDING,
    ACCEPTED,
    VOIDED,
    CANCELLED_REFUNDED
}

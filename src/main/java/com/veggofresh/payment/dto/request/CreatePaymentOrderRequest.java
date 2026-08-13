package com.veggofresh.payment.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * Request to create a Razorpay payment order.
 *
 * <p>The frontend calls this BEFORE opening the Razorpay checkout popup.
 * The backend creates a Razorpay order, returns the {@code razorpay_order_id}
 * and amount, which the frontend passes to the Razorpay JS SDK.
 *
 * <p>Flow: {@code GET /api/customer/orders/checkout/summary} (to see total)
 * → {@code POST /api/payment/orders} (this endpoint) → Razorpay popup.
 */
@Getter
@Setter
public class CreatePaymentOrderRequest {

    /**
     * ID of the delivery address the customer selected at checkout.
     * The backend uses this to compute the final order total (same as checkout summary).
     */
    @NotNull(message = "addressId is required")
    private UUID addressId;

    /**
     * Optional delivery slot ID selected by the customer.
     */
    private UUID deliverySlotId;

    /**
     * Optional scheduled delivery date in ISO format (e.g. "2026-08-15").
     */
    private String scheduledDate;
}

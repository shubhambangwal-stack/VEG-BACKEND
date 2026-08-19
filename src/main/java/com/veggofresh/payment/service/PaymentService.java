package com.veggofresh.payment.service;

import com.veggofresh.payment.dto.request.CreatePaymentOrderRequest;
import com.veggofresh.payment.dto.request.VerifyPaymentRequest;
import com.veggofresh.payment.dto.response.PaymentOrderResponse;
import com.veggofresh.payment.dto.response.PaymentVerifyResponse;
import com.veggofresh.payment.entity.Payment;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface PaymentService {

    /**
     * Step 1 of checkout: creates a Razorpay order with payment_capture=0 (Hold/Authorize mode)
     * and a VegGoFresh order in PAYMENT_PENDING state.
     */
    PaymentOrderResponse createPaymentOrder(UUID userId, CreatePaymentOrderRequest request);

    /**
     * Step 2 of checkout: verifies HMAC signature from Razorpay frontend callback.
     * Moves Payment to AUTHORIZED state. Order stays PAYMENT_PENDING until vendor accepts.
     */
    PaymentVerifyResponse verifyPayment(UUID userId, VerifyPaymentRequest request);

    /**
     * Called when a Vendor accepts an order.
     * Triggers Razorpay Capture API and finalizes wallet reservation.
     * Moves order PAYMENT_PENDING -> PLACED.
     */
    void capturePayment(UUID orderId, UUID vendorUserId, BigDecimal deliveryFee, BigDecimal commissionPercent);

    /**
     * Called when vendor-accept timeout expires.
     * Voids the Razorpay hold and releases customer wallet reservation.
     * Moves order PAYMENT_PENDING -> CANCELLED.
     */
    void voidPayment(UUID orderId);

    /**
     * Called when order is marked DELIVERED.
     * Credits Vendor, Delivery, and Admin wallets with their respective shares.
     */
    void settleOrderToWallets(UUID orderId, UUID vendorUserId, UUID deliveryUserId,
                              UUID adminUserId, BigDecimal vendorEarnings,
                              BigDecimal deliveryEarnings, BigDecimal adminCommission);

    /**
     * Handles Razorpay webhook events idempotently.
     * Supported: payment.authorized, payment.captured, payment.failed.
     */
    void handleWebhook(String payload, String signature);

    /** Returns all payment attempts for a given VegGoFresh order. */
    List<Payment> getPaymentsByOrder(UUID orderId, UUID userId);
}

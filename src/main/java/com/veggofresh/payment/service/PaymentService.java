package com.veggofresh.payment.service;

import com.veggofresh.payment.dto.request.CreatePaymentOrderRequest;
import com.veggofresh.payment.dto.request.VerifyPaymentRequest;
import com.veggofresh.payment.dto.response.PaymentOrderResponse;
import com.veggofresh.payment.dto.response.PaymentVerifyResponse;
import com.veggofresh.payment.entity.Payment;

import java.util.List;
import java.util.UUID;

public interface PaymentService {

    /**
     * Creates a Razorpay payment order and a VegGoFresh order in PAYMENT_PENDING state.
     *
     * <p>Steps:
     * <ol>
     *   <li>Validate cart is non-empty</li>
     *   <li>Compute order total (same as checkout summary)</li>
     *   <li>Call Razorpay API to create an order</li>
     *   <li>Persist a {@code Payment(CREATED)} row</li>
     *   <li>Persist a VegGoFresh {@code Order(PAYMENT_PENDING)} row</li>
     *   <li>Return {@code PaymentOrderResponse} with Razorpay order ID + amount for frontend</li>
     * </ol>
     *
     * @param userId  authenticated customer ID
     * @param request address, slot, and date for the order
     * @return Razorpay order details for frontend to open checkout popup
     */
    PaymentOrderResponse createPaymentOrder(UUID userId, CreatePaymentOrderRequest request);

    /**
     * Verifies Razorpay payment signature and places the VegGoFresh order.
     *
     * <p>Steps:
     * <ol>
     *   <li>Load {@code Payment} by {@code razorpayOrderId}</li>
     *   <li>HMAC-SHA256 verify: {@code razorpay_order_id + "|" + razorpay_payment_id} against keySecret</li>
     *   <li>Update Payment to CAPTURED, store payment ID + signature</li>
     *   <li>Move VegGoFresh order from PAYMENT_PENDING → PLACED</li>
     *   <li>Clear cart</li>
     *   <li>Return order response</li>
     * </ol>
     *
     * @param userId  authenticated customer ID
     * @param request Razorpay IDs and signature from frontend
     * @return placed order details
     * @throws com.veggofresh.platform.exception.BusinessException PAYMENT_SIGNATURE_INVALID on mismatch
     */
    PaymentVerifyResponse verifyPayment(UUID userId, VerifyPaymentRequest request);

    /**
     * Handles Razorpay webhook events idempotently.
     *
     * <p>Supported events:
     * <ul>
     *   <li>{@code payment.captured} — moves order to PLACED if not already done</li>
     *   <li>{@code payment.failed}   — marks Payment FAILED, order stays PAYMENT_PENDING</li>
     * </ul>
     *
     * <p>The {@code X-Razorpay-Signature} header is verified before any processing.
     * Duplicate events (same payment already CAPTURED) are silently ignored.
     *
     * @param payload   raw JSON body from Razorpay webhook
     * @param signature value of {@code X-Razorpay-Signature} header
     */
    void handleWebhook(String payload, String signature);

    /**
     * Returns all payment attempts for a given VegGoFresh order.
     * Used by the customer to see payment history (retry attempts etc.)
     */
    List<Payment> getPaymentsByOrder(UUID orderId, UUID userId);
}

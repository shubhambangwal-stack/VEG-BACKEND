package com.veggofresh.payment.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * Request to verify a Razorpay payment and place the VegGoFresh order.
 *
 * <p>After the customer pays via the Razorpay popup, the frontend receives
 * {@code razorpay_payment_id}, {@code razorpay_order_id}, and
 * {@code razorpay_signature} from Razorpay. It passes them here for
 * server-side signature verification.
 *
 * <p>On success: payment is marked CAPTURED, VegGoFresh order moves to PLACED,
 * cart is cleared.
 * On failure: 400 PAYMENT_SIGNATURE_INVALID — customer must retry.
 */
@Getter
@Setter
public class VerifyPaymentRequest {

    /**
     * Razorpay order ID (format: {@code order_XXXX}).
     * Must match the ID returned by {@code POST /api/payment/orders}.
     */
    @NotBlank(message = "razorpayOrderId is required")
    private String razorpayOrderId;

    /**
     * Razorpay payment ID (format: {@code pay_XXXX}).
     * Returned by Razorpay after customer completes payment.
     */
    @NotBlank(message = "razorpayPaymentId is required")
    private String razorpayPaymentId;

    /**
     * HMAC-SHA256 signature from Razorpay.
     * Computed by Razorpay as: HMAC_SHA256(razorpay_order_id + "|" + razorpay_payment_id, keySecret).
     * The backend recomputes this and compares — mismatch = tampered/invalid.
     */
    @NotBlank(message = "razorpaySignature is required")
    private String razorpaySignature;

    /**
     * The VegGoFresh order ID that was created in PAYMENT_PENDING state.
     * Used to look up the pending order and move it to PLACED on success.
     */
    @NotNull(message = "veggoOrderId is required")
    private UUID veggoOrderId;
}

package com.veggofresh.payment.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * Body of {@code POST /api/payment/orders/verify} -- the three fields
 * Razorpay's Checkout.js handler callback returns to the frontend on
 * successful payment. {@code razorpaySignature} is the HMAC-SHA256 of
 * {@code razorpayOrderId + "|" + razorpayPaymentId}, keyed with the
 * Razorpay key secret -- verifying it is what proves this call actually
 * came from a real Razorpay payment and wasn't forged by a client just
 * hitting the endpoint directly.
 */
@Getter
@Setter
public class VerifyPaymentRequestDto {

    @NotBlank(message = "razorpayOrderId is required")
    private String razorpayOrderId;

    @NotBlank(message = "razorpayPaymentId is required")
    private String razorpayPaymentId;

    @NotBlank(message = "razorpaySignature is required")
    private String razorpaySignature;
}

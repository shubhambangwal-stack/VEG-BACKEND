package com.veggofresh.payment.dto.response;

import com.veggofresh.customer.dto.response.OrderResponseDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response returned after payment signature verification.
 *
 * <p>On success: {@code success=true}, {@code order} is populated with the
 * placed VegGoFresh order (status PLACED). The frontend can redirect to the
 * order confirmation screen.
 *
 * <p>On failure: a {@code BusinessException} is thrown (HTTP 400) — this DTO
 * is never returned on failure.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentVerifyResponse {

    /** Always {@code true} in this DTO — failure throws an exception. */
    private boolean success;

    /** Razorpay payment ID (format: {@code pay_XXXX}). */
    private String razorpayPaymentId;

    /** The placed VegGoFresh order. Null only if order placement fails after capture (edge case). */
    private OrderResponseDto order;
}

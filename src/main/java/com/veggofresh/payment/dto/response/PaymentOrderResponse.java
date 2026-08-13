package com.veggofresh.payment.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Response returned after successfully creating a Razorpay payment order.
 *
 * <p>The frontend uses these fields to initialize the Razorpay JS checkout:
 * <pre>
 * var options = {
 *   key:      response.keyId,
 *   amount:   response.amountInPaise,
 *   currency: response.currency,
 *   order_id: response.razorpayOrderId,
 *   ...
 * };
 * var rzp = new Razorpay(options);
 * rzp.open();
 * </pre>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentOrderResponse {

    /** The VegGoFresh order ID (PAYMENT_PENDING state). Frontend needs this for verifyPayment(). */
    private UUID veggoOrderId;

    /** Razorpay order ID (format: {@code order_XXXX}). Pass to Razorpay JS SDK as {@code order_id}. */
    private String razorpayOrderId;

    /** Amount in INR (human-readable). For display purposes. */
    private BigDecimal amount;

    /** Amount in paise (INR × 100). Pass directly to Razorpay JS SDK as {@code amount}. */
    private long amountInPaise;

    /** Currency code, e.g. {@code INR}. */
    private String currency;

    /** Razorpay publishable API key ID. Pass to Razorpay JS SDK as {@code key}. */
    private String keyId;
}

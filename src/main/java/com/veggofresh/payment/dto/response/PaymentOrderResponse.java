package com.veggofresh.payment.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentOrderResponse {

    /** The VegGoFresh order ID (PAYMENT_PENDING state). */
    private UUID veggoOrderId;

    /** Razorpay order ID (format: order_XXXX). Null if fully wallet-covered. */
    private String razorpayOrderId;

    /** Total order amount in INR. */
    private BigDecimal amount;

    /** Amount charged to Razorpay in paise (after wallet deduction). */
    private long amountInPaise;

    /** Amount covered from the user wallet (INR). */
    private BigDecimal walletAmountUsed;

    /** Amount the user still needs to pay via Razorpay (INR). */
    private BigDecimal razorpayAmountToPay;

    /** Currency code, e.g. INR. */
    private String currency;

    /** Razorpay publishable key ID for the JS SDK. */
    private String keyId;
}

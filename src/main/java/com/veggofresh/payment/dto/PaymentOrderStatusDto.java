package com.veggofresh.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentOrderStatusDto {
    private UUID paymentOrderId;
    private String razorpayOrderId;
    private String status;
    private BigDecimal totalAmount;
    private BigDecimal capturedAmount;
    private String currency;
    private Instant authorizedAt;
    private Instant capturedAt;
    private List<PaymentOrderLineStatusDto> lines;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentOrderLineStatusDto {
        private UUID orderId;
        private BigDecimal amount;
        private String status;
    }
}

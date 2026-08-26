package com.veggofresh.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/** One Customer Order's slice of a {@link PaymentHoldResponseDto}'s total. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentOrderLineAllocationDto {
    private UUID orderId;
    private String orderNumber;
    private BigDecimal amount;
}

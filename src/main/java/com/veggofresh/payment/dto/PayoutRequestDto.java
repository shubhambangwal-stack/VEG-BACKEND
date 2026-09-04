package com.veggofresh.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayoutRequestDto {
    private UUID id;
    private BigDecimal amount;
    private String status;
    private String adminNotes;
    private Instant createdAt;
    private Instant processedAt;
    private String razorpayPayoutId;
}

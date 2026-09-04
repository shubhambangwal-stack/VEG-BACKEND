package com.veggofresh.payment.dto;

import com.veggofresh.payment.entity.PayoutRequestStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayoutResponseDto {

    private UUID id;
    private UUID userId;
    private String userRole;
    private BigDecimal amount;
    private PayoutRequestStatus status;
    private String razorpayPayoutId;
    private String adminNotes;
    private String failureReason;
    private UserBankAccountDto bankAccount;
    private Instant processedAt;
    private Instant createdAt;
}

package com.veggofresh.payment.dto.response;

import com.veggofresh.payment.entity.WalletTransactionType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class WalletTransactionResponse {
    private UUID id;
    private UUID orderId;
    private String razorpayPaymentId;
    private WalletTransactionType type;
    private BigDecimal amount;
    private String description;
    private Instant createdAt;
}

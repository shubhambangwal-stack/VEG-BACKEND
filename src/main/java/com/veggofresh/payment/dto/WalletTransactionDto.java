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
public class WalletTransactionDto {
    private UUID id;
    private String type;       // CREDIT / DEBIT
    private String reason;     // WalletTransactionReason name
    private BigDecimal amount;
    private BigDecimal balanceAfter;
    private UUID referenceId;
    private String description;
    private Instant createdAt;
}

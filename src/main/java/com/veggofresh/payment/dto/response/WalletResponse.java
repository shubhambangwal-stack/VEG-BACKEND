package com.veggofresh.payment.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class WalletResponse {
    private UUID walletId;
    private UUID userId;
    private String role;
    private BigDecimal availableBalance;
    private BigDecimal reservedBalance;
    /** Total = available + reserved */
    private BigDecimal totalBalance;
}

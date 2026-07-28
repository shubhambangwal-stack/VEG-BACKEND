package com.veggofresh.delivery.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class EarningsSummaryResponseDto {
    private String period;
    private BigDecimal totalEarnings;
    private long totalDeliveries;
    private List<EarningEntryDto> entries;

    @Getter
    @Builder
    public static class EarningEntryDto {
        private UUID orderId;
        private BigDecimal amount;
        private Instant earnedAt;
    }
}

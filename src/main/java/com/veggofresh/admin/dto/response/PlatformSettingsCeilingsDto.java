package com.veggofresh.admin.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlatformSettingsCeilingsDto {
    private double maxDeliveryRadiusKm;
    private BigDecimal maxPlatformCommissionPercent;
    private int maxAcceptTimeoutSeconds;
    private int maxRebroadcastRounds;
    private int maxRebroadcastElapsedMinutes;
}

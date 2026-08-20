package com.veggofresh.admin.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlatformSettingsResponseDto {
    private double deliveryRadiusKm;
    private BigDecimal platformCommissionPercent;
    private int vendorAcceptTimeoutSeconds;
    private int deliveryAcceptTimeoutSeconds;
    private int rebroadcastMaxRounds;
    private int rebroadcastMaxElapsedMinutes;

    /** Echoes the hard ceilings back so the Admin UI can show "max allowed: X" inline, without hardcoding them client-side too. */
    private PlatformSettingsCeilingsDto ceilings;

    private Instant updatedAt;
}

package com.veggofresh.admin.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Partial update DTO for PATCH /api/admin/settings/fees.
 * Only the two fee amounts -- admin can update just these without
 * supplying every other platform setting (unlike the full PUT).
 */
@Getter
@Setter
public class PlatformFeesUpdateRequestDto {

    @NotNull(message = "platformFeeAmount is required")
    @DecimalMin(value = "0.0", message = "platformFeeAmount cannot be negative")
    private BigDecimal platformFeeAmount;

    @NotNull(message = "deliveryFeeAmount is required")
    @DecimalMin(value = "0.0", message = "deliveryFeeAmount cannot be negative")
    private BigDecimal deliveryFeeAmount;
}

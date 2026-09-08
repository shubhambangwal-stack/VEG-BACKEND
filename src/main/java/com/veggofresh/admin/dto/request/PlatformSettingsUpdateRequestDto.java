package com.veggofresh.admin.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * All fields required (PUT semantics, not PATCH -- unlike Customer's profile update,
 * these settings are few enough and consequential enough that partial updates would
 * risk an Admin thinking they changed one value while others silently stayed stale).
 *
 * Bean validation here covers "obviously invalid" (negative, zero). The real hard
 * ceilings (PROJECT_STATE's "hard upper bound enforced in code") are enforced in
 * PlatformSettingsServiceImpl, not here -- Bean Validation annotations alone would
 * hardcode the ceiling values in two places (annotation + service) with no single
 * source of truth; the service is that single source.
 */
@Getter
@Setter
public class PlatformSettingsUpdateRequestDto {

    @NotNull(message = "deliveryRadiusKm is required")
    @DecimalMin(value = "0.5", message = "deliveryRadiusKm must be at least 0.5km")
    private Double deliveryRadiusKm;

    @NotNull(message = "platformCommissionPercent is required")
    @DecimalMin(value = "0.0", message = "platformCommissionPercent cannot be negative")
    @DecimalMax(value = "100.0", message = "platformCommissionPercent cannot exceed 100")
    private BigDecimal platformCommissionPercent;

    @NotNull(message = "vendorAcceptTimeoutSeconds is required")
    @Min(value = 30, message = "vendorAcceptTimeoutSeconds must be at least 30 seconds")
    private Integer vendorAcceptTimeoutSeconds;

    @NotNull(message = "deliveryAcceptTimeoutSeconds is required")
    @Min(value = 15, message = "deliveryAcceptTimeoutSeconds must be at least 15 seconds")
    private Integer deliveryAcceptTimeoutSeconds;

    @NotNull(message = "rebroadcastMaxRounds is required")
    @Min(value = 1, message = "rebroadcastMaxRounds must be at least 1")
    private Integer rebroadcastMaxRounds;

    @NotNull(message = "rebroadcastMaxElapsedMinutes is required")
    @Min(value = 1, message = "rebroadcastMaxElapsedMinutes must be at least 1")
    private Integer rebroadcastMaxElapsedMinutes;

    /**
     * Deliberately NO @Max here, unlike every other field above -- confirmed with the
     * team this one has no hard ceiling; whatever Admin sets is used as-is. Only a
     * floor is enforced (must be positive), as plain input validation, not a business rule.
     */
    @NotNull(message = "otpExpiryMinutes is required")
    @Min(value = 1, message = "otpExpiryMinutes must be at least 1 minute")
    private Integer otpExpiryMinutes;
}

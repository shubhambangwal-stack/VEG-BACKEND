package com.veggofresh.admin.service;

import com.veggofresh.admin.dto.request.PlatformSettingsUpdateRequestDto;
import com.veggofresh.admin.dto.response.PlatformSettingsResponseDto;

import java.math.BigDecimal;

/**
 * Cross-module interface -- Vendor and Delivery read platform configuration through
 * this, never by importing PlatformSettings @Entity directly. Individual scalar getters
 * exist alongside getSettings() so a caller that only needs one value (e.g. Vendor's
 * ProductCatalogServiceImpl only ever needs the radius) doesn't have to round-trip a
 * full DTO build for it.
 */
public interface PlatformSettingsService {

    PlatformSettingsResponseDto getSettings();

    PlatformSettingsResponseDto updateSettings(PlatformSettingsUpdateRequestDto request);

    double getDeliveryRadiusKm();

    BigDecimal getPlatformCommissionPercent();

    int getVendorAcceptTimeoutSeconds();

    int getDeliveryAcceptTimeoutSeconds();

    int getRebroadcastMaxRounds();

    int getRebroadcastMaxElapsedMinutes();

    /** Used by Delivery when issuing/regenerating pickup and drop OTPs. No hard ceiling -- whatever Admin sets is used as-is. */
    int getOtpExpiryMinutes();
}

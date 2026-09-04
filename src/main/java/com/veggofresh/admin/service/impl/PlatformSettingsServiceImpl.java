package com.veggofresh.admin.service.impl;

import com.veggofresh.admin.dto.request.PlatformSettingsUpdateRequestDto;
import com.veggofresh.admin.dto.response.PlatformSettingsCeilingsDto;
import com.veggofresh.admin.dto.response.PlatformSettingsResponseDto;
import com.veggofresh.admin.entity.PlatformSettings;
import com.veggofresh.admin.repository.PlatformSettingsRepository;
import com.veggofresh.admin.service.PlatformSettingsService;
import com.veggofresh.platform.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * HARD CEILINGS (PROJECT_STATE: "hard upper bound enforced in code, not just
 * documented"). These are enforced HERE, on write -- not just as Bean Validation
 * annotations on the request DTO, and not as a silent clamp. An Admin request that
 * exceeds a ceiling is REJECTED with a clear error naming the ceiling, not quietly
 * capped -- so Admin always knows exactly what's actually saved.
 *
 * Rationale for each ceiling:
 * - Accept timeouts (vendor + delivery) capped at 30 minutes: directly from the
 *   Payment design's own reasoning -- Razorpay authorized-but-uncaptured holds
 *   auto-refund after 5 days regardless, but a customer should never realistically be
 *   waiting half an hour just for someone to accept a single round.
 * - Rebroadcast max rounds capped at 20, max elapsed at 120 minutes: bounds the
 *   *total* worst-case customer wait across every round combined, independent of the
 *   per-round timeout above.
 * - Radius capped at 50km, commission at 50%: sanity ceilings against Admin fat-finger
 *   input (e.g. typing 500 instead of 50), not derived from any specific design
 *   requirement.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class PlatformSettingsServiceImpl implements PlatformSettingsService {

    public static final double MAX_DELIVERY_RADIUS_KM = 50.0;
    public static final BigDecimal MAX_PLATFORM_COMMISSION_PERCENT = BigDecimal.valueOf(50.0);
    public static final int MAX_ACCEPT_TIMEOUT_SECONDS = 1800; // 30 minutes
    public static final int MAX_REBROADCAST_ROUNDS = 20;
    public static final int MAX_REBROADCAST_ELAPSED_MINUTES = 120; // 2 hours

    private final PlatformSettingsRepository platformSettingsRepository;

    @Override
    @Transactional(readOnly = true)
    public PlatformSettingsResponseDto getSettings() {
        return mapToDto(getOrCreateSettings());
    }

    @Override
    public PlatformSettingsResponseDto updateSettings(PlatformSettingsUpdateRequestDto request) {
        if (request.getDeliveryRadiusKm() > MAX_DELIVERY_RADIUS_KM) {
            throw new BusinessException("SETTINGS_RADIUS_TOO_HIGH",
                    "deliveryRadiusKm cannot exceed " + MAX_DELIVERY_RADIUS_KM + "km", HttpStatus.BAD_REQUEST);
        }
        if (request.getPlatformCommissionPercent().compareTo(MAX_PLATFORM_COMMISSION_PERCENT) > 0) {
            throw new BusinessException("SETTINGS_COMMISSION_TOO_HIGH",
                    "platformCommissionPercent cannot exceed " + MAX_PLATFORM_COMMISSION_PERCENT + "%", HttpStatus.BAD_REQUEST);
        }
        if (request.getVendorAcceptTimeoutSeconds() > MAX_ACCEPT_TIMEOUT_SECONDS) {
            throw new BusinessException("SETTINGS_VENDOR_TIMEOUT_TOO_HIGH",
                    "vendorAcceptTimeoutSeconds cannot exceed " + MAX_ACCEPT_TIMEOUT_SECONDS + " seconds (30 minutes)", HttpStatus.BAD_REQUEST);
        }
        if (request.getDeliveryAcceptTimeoutSeconds() > MAX_ACCEPT_TIMEOUT_SECONDS) {
            throw new BusinessException("SETTINGS_DELIVERY_TIMEOUT_TOO_HIGH",
                    "deliveryAcceptTimeoutSeconds cannot exceed " + MAX_ACCEPT_TIMEOUT_SECONDS + " seconds (30 minutes)", HttpStatus.BAD_REQUEST);
        }
        if (request.getRebroadcastMaxRounds() > MAX_REBROADCAST_ROUNDS) {
            throw new BusinessException("SETTINGS_REBROADCAST_ROUNDS_TOO_HIGH",
                    "rebroadcastMaxRounds cannot exceed " + MAX_REBROADCAST_ROUNDS, HttpStatus.BAD_REQUEST);
        }
        if (request.getRebroadcastMaxElapsedMinutes() > MAX_REBROADCAST_ELAPSED_MINUTES) {
            throw new BusinessException("SETTINGS_REBROADCAST_ELAPSED_TOO_HIGH",
                    "rebroadcastMaxElapsedMinutes cannot exceed " + MAX_REBROADCAST_ELAPSED_MINUTES + " minutes (2 hours)", HttpStatus.BAD_REQUEST);
        }

        PlatformSettings settings = getOrCreateSettings();
        settings.setDeliveryRadiusKm(request.getDeliveryRadiusKm());
        settings.setPlatformCommissionPercent(request.getPlatformCommissionPercent());
        settings.setVendorAcceptTimeoutSeconds(request.getVendorAcceptTimeoutSeconds());
        settings.setDeliveryAcceptTimeoutSeconds(request.getDeliveryAcceptTimeoutSeconds());
        settings.setRebroadcastMaxRounds(request.getRebroadcastMaxRounds());
        settings.setRebroadcastMaxElapsedMinutes(request.getRebroadcastMaxElapsedMinutes());
        // Deliberately NO ceiling check here, unlike every field above -- confirmed with
        // the team this one has no hard upper bound; Bean Validation's @Min(1) on the
        // request DTO is the only guard (must be positive).
        settings.setOtpExpiryMinutes(request.getOtpExpiryMinutes());

        return mapToDto(platformSettingsRepository.save(settings));
    }

    @Override
    @Transactional(readOnly = true)
    public double getDeliveryRadiusKm() {
        return getOrCreateSettings().getDeliveryRadiusKm();
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getPlatformCommissionPercent() {
        return getOrCreateSettings().getPlatformCommissionPercent();
    }

    @Override
    @Transactional(readOnly = true)
    public int getVendorAcceptTimeoutSeconds() {
        return getOrCreateSettings().getVendorAcceptTimeoutSeconds();
    }

    @Override
    @Transactional(readOnly = true)
    public int getDeliveryAcceptTimeoutSeconds() {
        return getOrCreateSettings().getDeliveryAcceptTimeoutSeconds();
    }

    @Override
    @Transactional(readOnly = true)
    public int getRebroadcastMaxRounds() {
        return getOrCreateSettings().getRebroadcastMaxRounds();
    }

    @Override
    @Transactional(readOnly = true)
    public int getRebroadcastMaxElapsedMinutes() {
        return getOrCreateSettings().getRebroadcastMaxElapsedMinutes();
    }

    @Override
    @Transactional(readOnly = true)
    public int getOtpExpiryMinutes() {
        return getOrCreateSettings().getOtpExpiryMinutes();
    }

    // ─────────────────────────────────────────────────────────────────────
    // PRIVATE HELPERS
    // ─────────────────────────────────────────────────────────────────────

    /** Single-row table -- auto-creates with defaults on first access, same getOrCreate pattern as CustomerProfile. */
    private PlatformSettings getOrCreateSettings() {
        return platformSettingsRepository.findAll().stream()
                .findFirst()
                .orElseGet(() -> platformSettingsRepository.saveAndFlush(new PlatformSettings()));
    }

    private PlatformSettingsResponseDto mapToDto(PlatformSettings settings) {
        return PlatformSettingsResponseDto.builder()
                .deliveryRadiusKm(settings.getDeliveryRadiusKm())
                .platformCommissionPercent(settings.getPlatformCommissionPercent())
                .vendorAcceptTimeoutSeconds(settings.getVendorAcceptTimeoutSeconds())
                .deliveryAcceptTimeoutSeconds(settings.getDeliveryAcceptTimeoutSeconds())
                .rebroadcastMaxRounds(settings.getRebroadcastMaxRounds())
                .rebroadcastMaxElapsedMinutes(settings.getRebroadcastMaxElapsedMinutes())
                .otpExpiryMinutes(settings.getOtpExpiryMinutes())
                .ceilings(PlatformSettingsCeilingsDto.builder()
                        .maxDeliveryRadiusKm(MAX_DELIVERY_RADIUS_KM)
                        .maxPlatformCommissionPercent(MAX_PLATFORM_COMMISSION_PERCENT)
                        .maxAcceptTimeoutSeconds(MAX_ACCEPT_TIMEOUT_SECONDS)
                        .maxRebroadcastRounds(MAX_REBROADCAST_ROUNDS)
                        .maxRebroadcastElapsedMinutes(MAX_REBROADCAST_ELAPSED_MINUTES)
                        .build())
                .updatedAt(settings.getUpdatedAt())
                .build();
    }
}

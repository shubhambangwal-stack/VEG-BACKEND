package com.veggofresh.admin.service.impl;

import com.veggofresh.admin.dto.request.PlatformSettingsUpdateRequestDto;
import com.veggofresh.admin.dto.response.PlatformSettingsCeilingsDto;
import com.veggofresh.admin.dto.response.PlatformSettingsResponseDto;
import com.veggofresh.admin.entity.PlatformSettings;
import com.veggofresh.admin.repository.PlatformSettingsRepository;
import com.veggofresh.admin.service.PlatformSettingsService;
import com.veggofresh.platform.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

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
 * - Radius capped at 50km: sanity ceiling against Admin fat-finger input.
 * - platformFeeAmount capped at ₹500, deliveryFeeAmount capped at ₹500:
 *   sanity ceilings to prevent accidental fat-finger values.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlatformSettingsServiceImpl implements PlatformSettingsService {

    public static final double MAX_DELIVERY_RADIUS_KM = 50.0;
    public static final BigDecimal MAX_PLATFORM_FEE_AMOUNT = BigDecimal.valueOf(500.00);
    public static final BigDecimal MAX_DELIVERY_FEE_AMOUNT = BigDecimal.valueOf(500.00);
    public static final int MAX_ACCEPT_TIMEOUT_SECONDS = 1800; // 30 minutes
    public static final int MAX_REBROADCAST_ROUNDS = 20;
    public static final int MAX_REBROADCAST_ELAPSED_MINUTES = 120; // 2 hours

    private final PlatformSettingsRepository platformSettingsRepository;

    /**
     * In-process memo of the singleton settings row. Read-mostly by a wide
     * margin, and these getters are called from inside other modules' write
     * transactions (twice per cart when mapping a cart response), so keeping
     * them off the database is both a correctness and a latency win.
     * Non-volatile on purpose: a benign race just re-reads the row. Updated
     * only by {@link #updateSettings}, which also writes through the
     * repository, so a multi-instance deployment converges on the next
     * read-through after each instance's own write.
     */
    private PlatformSettings cachedSettings;

    @Override
    @Transactional(readOnly = true)
    public PlatformSettingsResponseDto getSettings() {
        return mapToDto(settingsOrDefaults());
    }

    /**
     * Read-only accessor for the singleton row, memoised in-process.
     *
     * <p>These getters are called from inside other modules' write transactions
     * (e.g. twice per cart while mapping a cart response), so they must be cheap
     * and must never write. Caching also keeps them from issuing two SELECTs per
     * cart per request, which is what made the old auto-creating version hot
     * enough to hit the INSERT race in the first place.
     *
     * <p>Invalidated by {@link #updateSettings} on write.
     */
    private PlatformSettings settingsOrDefaults() {
        PlatformSettings cached = cachedSettings;
        if (cached != null) {
            return cached;
        }
        PlatformSettings loaded = findSettings().orElseGet(() -> {
            // No row yet (should not happen post-V161). Return entity defaults
            // WITHOUT writing -- creating the row here is exactly the bug this
            // class is being fixed for. Admin's own update endpoint will persist
            // a real row on first save.
            log.warn("platform_settings row missing; serving entity defaults. "
                    + "Check that V161__seed_platform_settings_row.sql was applied.");
            return new PlatformSettings();
        });
        cachedSettings = loaded;
        return loaded;
    }

    @Override
    @Transactional
    public PlatformSettingsResponseDto updateSettings(PlatformSettingsUpdateRequestDto request) {
        if (request.getDeliveryRadiusKm() > MAX_DELIVERY_RADIUS_KM) {
            throw new BusinessException("SETTINGS_RADIUS_TOO_HIGH",
                    "deliveryRadiusKm cannot exceed " + MAX_DELIVERY_RADIUS_KM + "km", HttpStatus.BAD_REQUEST);
        }
        if (request.getPlatformFeeAmount().compareTo(MAX_PLATFORM_FEE_AMOUNT) > 0) {
            throw new BusinessException("SETTINGS_PLATFORM_FEE_TOO_HIGH",
                    "platformFeeAmount cannot exceed \u20b9" + MAX_PLATFORM_FEE_AMOUNT, HttpStatus.BAD_REQUEST);
        }
        if (request.getDeliveryFeeAmount().compareTo(MAX_DELIVERY_FEE_AMOUNT) > 0) {
            throw new BusinessException("SETTINGS_DELIVERY_FEE_TOO_HIGH",
                    "deliveryFeeAmount cannot exceed \u20b9" + MAX_DELIVERY_FEE_AMOUNT, HttpStatus.BAD_REQUEST);
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

        PlatformSettings settings = findSettings().orElseGet(PlatformSettings::new);
        settings.setDeliveryRadiusKm(request.getDeliveryRadiusKm());
        settings.setPlatformFeeAmount(request.getPlatformFeeAmount());
        settings.setDeliveryFeeAmount(request.getDeliveryFeeAmount());
        settings.setVendorAcceptTimeoutSeconds(request.getVendorAcceptTimeoutSeconds());
        settings.setDeliveryAcceptTimeoutSeconds(request.getDeliveryAcceptTimeoutSeconds());
        settings.setRebroadcastMaxRounds(request.getRebroadcastMaxRounds());
        settings.setRebroadcastMaxElapsedMinutes(request.getRebroadcastMaxElapsedMinutes());
        // Deliberately NO ceiling check here, unlike every field above -- confirmed with
        // the team this one has no hard upper bound; Bean Validation's @Min(1) on the
        // request DTO is the only guard (must be positive).
        settings.setOtpExpiryMinutes(request.getOtpExpiryMinutes());

        PlatformSettings saved = platformSettingsRepository.save(settings);
        cachedSettings = saved;
        return mapToDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public double getDeliveryRadiusKm() {
        return settingsOrDefaults().getDeliveryRadiusKm();
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getPlatformFeeAmount() {
        return settingsOrDefaults().getPlatformFeeAmount();
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getDeliveryFeeAmount() {
        return settingsOrDefaults().getDeliveryFeeAmount();
    }

    @Override
    @Transactional(readOnly = true)
    public int getVendorAcceptTimeoutSeconds() {
        return settingsOrDefaults().getVendorAcceptTimeoutSeconds();
    }

    @Override
    @Transactional(readOnly = true)
    public int getDeliveryAcceptTimeoutSeconds() {
        return settingsOrDefaults().getDeliveryAcceptTimeoutSeconds();
    }

    @Override
    @Transactional(readOnly = true)
    public int getRebroadcastMaxRounds() {
        return settingsOrDefaults().getRebroadcastMaxRounds();
    }

    @Override
    @Transactional(readOnly = true)
    public int getRebroadcastMaxElapsedMinutes() {
        return settingsOrDefaults().getRebroadcastMaxElapsedMinutes();
    }

    @Override
    @Transactional(readOnly = true)
    public int getOtpExpiryMinutes() {
        return settingsOrDefaults().getOtpExpiryMinutes();
    }

    // ─────────────────────────────────────────────────────────────────────
    // PRIVATE HELPERS
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Single-row table. V161__seed_platform_settings_row.sql now seeds the row,
     * so the READ path must never write.
     *
     * <p>Previously this method did {@code orElseGet(() -> saveAndFlush(new
     * PlatformSettings()))} and was called from every getter below -- all of
     * which are declared {@code readOnly = true}. That made it the only
     * {@code saveAndFlush} in the codebase living inside a read-declared method,
     * and it caused {@code UnexpectedRollbackException} on
     * {@code POST /api/customer/carts/items}: {@code CartServiceImpl.mapToDto}
     * reads the delivery/platform fees once per cart, from inside
     * {@code addItemToCart}'s write transaction. Two problems:
     * <ol>
     *   <li>{@code saveAndFlush} forces an immediate flush of the whole
     *       persistence context, flushing the cart's still-pending
     *       {@code Cart}/{@code CartItem} inserts mid-business-logic.</li>
     *   <li>On the previously-unseeded table, concurrent requests both saw zero
     *       rows and both tried to INSERT the singleton. The loser's failure
     *       escaped a nested {@code @Transactional} method, which makes Spring
     *       mark the shared transaction rollback-only; the caller swallowed it and
     *       returned "successfully", so it only surfaced at commit.</li>
     * </ol>
     *
     * <p>Reading these values is now a pure SELECT, so it can never poison a
     * caller's transaction.
     */
    private Optional<PlatformSettings> findSettings() {
        return platformSettingsRepository.findAll().stream().findFirst();
    }

    private PlatformSettingsResponseDto mapToDto(PlatformSettings settings) {
        return PlatformSettingsResponseDto.builder()
                .deliveryRadiusKm(settings.getDeliveryRadiusKm())
                .platformFeeAmount(settings.getPlatformFeeAmount())
                .deliveryFeeAmount(settings.getDeliveryFeeAmount())
                .vendorAcceptTimeoutSeconds(settings.getVendorAcceptTimeoutSeconds())
                .deliveryAcceptTimeoutSeconds(settings.getDeliveryAcceptTimeoutSeconds())
                .rebroadcastMaxRounds(settings.getRebroadcastMaxRounds())
                .rebroadcastMaxElapsedMinutes(settings.getRebroadcastMaxElapsedMinutes())
                .otpExpiryMinutes(settings.getOtpExpiryMinutes())
                .ceilings(PlatformSettingsCeilingsDto.builder()
                        .maxDeliveryRadiusKm(MAX_DELIVERY_RADIUS_KM)
                        .maxPlatformFeeAmount(MAX_PLATFORM_FEE_AMOUNT)
                        .maxDeliveryFeeAmount(MAX_DELIVERY_FEE_AMOUNT)
                        .maxAcceptTimeoutSeconds(MAX_ACCEPT_TIMEOUT_SECONDS)
                        .maxRebroadcastRounds(MAX_REBROADCAST_ROUNDS)
                        .maxRebroadcastElapsedMinutes(MAX_REBROADCAST_ELAPSED_MINUTES)
                        .build())
                .updatedAt(settings.getUpdatedAt())
                .build();
    }
}

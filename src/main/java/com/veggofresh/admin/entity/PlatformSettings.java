package com.veggofresh.admin.entity;

import com.veggofresh.platform.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Single-row platform-wide configuration. Admin edits this via AdminSettingsController;
 * every other module reads it through PlatformSettingsService (cross-module interface),
 * never this entity directly.
 *
 * HARD CEILINGS (PROJECT_STATE, Payment/broadcast design, "hard upper bound enforced in
 * code, not just documented"): every field here has a matching MAX_* constant on
 * PlatformSettingsServiceImpl that Admin's own update endpoint enforces server-side,
 * regardless of what value is requested. A misconfigured Admin setting cannot hold a
 * customer's funds, or leave an order broadcasting, for an unreasonable amount of time.
 */
@Entity
@Table(name = "platform_settings")
@Getter
@Setter
public class PlatformSettings extends BaseEntity {

    /** Hard cutoff radius (km) for both customer<->vendor and vendor<->delivery-partner matching. Same value, both legs. */
    @Column(name = "delivery_radius_km", nullable = false)
    private double deliveryRadiusKm = 10.0;

    /** Platform's cut of each order, as a percentage (0-100). Replaces Vendor's flat 10% placeholder. */
    @Column(name = "platform_commission_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal platformCommissionPercent = BigDecimal.valueOf(10.0);

    /** How long a vendor has to accept an order before it re-broadcasts/times out. */
    @Column(name = "vendor_accept_timeout_seconds", nullable = false)
    private int vendorAcceptTimeoutSeconds = 300;

    /** How long a delivery partner has to accept a dispatched assignment before it re-broadcasts/times out. */
    @Column(name = "delivery_accept_timeout_seconds", nullable = false)
    private int deliveryAcceptTimeoutSeconds = 60;

    /** Max re-broadcast rounds after a cancel-after-accept or a round timing out with nobody accepting. Applies to both vendor-broadcast and delivery-broadcast legs. */
    @Column(name = "rebroadcast_max_rounds", nullable = false)
    private int rebroadcastMaxRounds = 5;

    /** Max total elapsed time (minutes) across ALL re-broadcast rounds combined, independent of the max-rounds cap above -- whichever limit is hit first wins. */
    @Column(name = "rebroadcast_max_elapsed_minutes", nullable = false)
    private int rebroadcastMaxElapsedMinutes = 30;

    /**
     * How long a pickup or drop OTP (Delivery module) stays valid before it must be
     * regenerated. Deliberately has NO hard ceiling in PlatformSettingsServiceImpl,
     * unlike every other field on this entity -- Admin's own call, whatever value is
     * set is used as-is. Only a basic floor (must be positive) is enforced, as plain
     * input validation rather than a business ceiling.
     */
    @Column(name = "otp_expiry_minutes", nullable = false)
    private int otpExpiryMinutes = 120;
}

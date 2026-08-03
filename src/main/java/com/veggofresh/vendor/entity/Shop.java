package com.veggofresh.vendor.entity;

import com.veggofresh.platform.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "vendor_shops")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Shop extends BaseEntity {

    @Column(name = "owner_user_id", nullable = false)
    private UUID ownerUserId;

    @Column(nullable = false)
    private String name;

    /** Legacy single-string address. Kept for backward compatibility with existing
     *  callers (ProductCatalogService, ShopDto display) -- auto-populated as a
     *  concatenation of the structured fields below whenever they're set. */
    @Column(columnDefinition = "TEXT")
    private String address;

    @Column
    private Double latitude;

    @Column
    private Double longitude;

    @Enumerated(EnumType.STRING)
    @Column(name = "kyc_status", nullable = false)
    @Builder.Default
    private KycStatus kycStatus = KycStatus.PENDING;

    @Column(name = "is_online", nullable = false)
    @Builder.Default
    private boolean isOnline = false;

    // ── Onboarding: Basic Info (Step 1) ─────────────────────────────────
    // fullName/email are a WORKAROUND -- Auth's User entity has no name field,
    // and email updates have no cross-module path from Vendor. Same pattern as
    // Delivery's DeliveryPartnerProfile.fullName. See NOTES_VENDOR.md.
    @Column(name = "full_name", length = 255)
    private String fullName;

    @Column(length = 255)
    private String email;

    @Column(name = "business_phone", length = 20)
    private String businessPhone;

    @Column(name = "business_type", length = 100)
    private String businessType;

    @Column(name = "has_basic_info", nullable = false)
    @Builder.Default
    private boolean hasBasicInfo = false;

    // ── Onboarding: Business Location (Step 2) ──────────────────────────
    @Column(name = "street_address", length = 255)
    private String streetAddress;

    @Column(length = 100)
    private String city;

    @Column(length = 100)
    private String state;

    @Column(name = "zip_code", length = 20)
    private String zipCode;

    @Column(name = "has_business_location", nullable = false)
    @Builder.Default
    private boolean hasBusinessLocation = false;

    // ── Onboarding: Verification & Documentation (Step 3) ───────────────
    @Column(name = "application_submitted_at")
    private Instant applicationSubmittedAt;

    @Column(name = "kyc_rejection_reason", length = 1000)
    private String kycRejectionReason;

    // ── Post-approval "getting started" checklist ───────────────────────
    @Column(name = "delivery_range_km")
    private Double deliveryRangeKm;

    // Stub for now -- Payment module doesn't exist. Boolean only; when the real
    // "Configure Payment Settings" screen is built, add structured bank/Razorpay
    // Linked Account fields (ifscCode, accountNumber, beneficiaryName) alongside
    // this rather than replacing it. See NOTES_VENDOR.md for the Razorpay Route
    // field mapping this was designed against.
    @Column(name = "payment_settings_configured", nullable = false)
    @Builder.Default
    private boolean paymentSettingsConfigured = false;

    // ── Store Profile (Figma "Store Profile Management") ───────────────
    @Column(name = "store_image_url", columnDefinition = "TEXT")
    private String storeImageUrl;

    @Column(name = "store_bio", columnDefinition = "TEXT")
    private String storeBio;

    /** Semicolon-separated tags (e.g. "Organic;Locally Sourced;Eco-friendly"),
     *  same lightweight pattern as Delivery/Product's whyItsGreat field --
     *  a handful of display badges doesn't warrant a separate table. */
    @Column(name = "store_attributes", columnDefinition = "TEXT")
    private String storeAttributes;

    // ── Account Settings (Figma "Vendor Account Settings") ──────────────
    // profileImageUrl is the OWNER's personal photo, distinct from storeImageUrl
    // (the storefront photo shown to customers).
    @Column(name = "profile_image_url", columnDefinition = "TEXT")
    private String profileImageUrl;

    @Column(name = "business_license_number", length = 100)
    private String businessLicenseNumber;

    @Column(name = "new_order_alerts_enabled", nullable = false)
    @Builder.Default
    private boolean newOrderAlertsEnabled = true;

    @Column(name = "low_stock_notifications_enabled", nullable = false)
    @Builder.Default
    private boolean lowStockNotificationsEnabled = true;

    @Column(name = "payout_confirmations_enabled", nullable = false)
    @Builder.Default
    private boolean payoutConfirmationsEnabled = false;
}

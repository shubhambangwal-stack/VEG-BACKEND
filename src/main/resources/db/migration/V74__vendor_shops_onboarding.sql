-- ============================================================
-- VegGo Fresh — Vendor Module (V70-V89 range)
-- V74: Onboarding fields on vendor_shops
--
-- full_name/email are a WORKAROUND -- Auth's User entity has no name field and
-- no cross-module email-update path exists. Same pattern as Delivery's
-- DeliveryPartnerProfile.fullName. See NOTES_VENDOR.md.
--
-- payment_settings_configured is a stub boolean -- Payment module doesn't exist
-- yet. Designed to be extended (not replaced) with structured bank/Razorpay
-- Linked Account fields later. See NOTES_VENDOR.md.
-- ============================================================

ALTER TABLE vendor_shops
    ADD COLUMN full_name VARCHAR(255) NULL,
    ADD COLUMN email VARCHAR(255) NULL,
    ADD COLUMN business_phone VARCHAR(20) NULL,
    ADD COLUMN business_type VARCHAR(100) NULL,
    ADD COLUMN has_basic_info BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN street_address VARCHAR(255) NULL,
    ADD COLUMN city VARCHAR(100) NULL,
    ADD COLUMN state VARCHAR(100) NULL,
    ADD COLUMN zip_code VARCHAR(20) NULL,
    ADD COLUMN has_business_location BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN application_submitted_at TIMESTAMP(6) NULL,
    ADD COLUMN kyc_rejection_reason VARCHAR(1000) NULL,
    ADD COLUMN delivery_range_km DOUBLE PRECISION NULL,
    ADD COLUMN payment_settings_configured BOOLEAN NOT NULL DEFAULT FALSE;

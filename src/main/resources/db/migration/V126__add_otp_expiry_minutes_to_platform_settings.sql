-- ============================================================
-- VegGo Fresh Platform — Flyway Migration V126
-- Admin Module: Add otp_expiry_minutes to platform_settings
--
-- Replaces the hardcoded OTP_EXPIRY_MINUTES = 15 constant previously in
-- DeliveryAssignmentServiceImpl -- now Admin-configurable, applies to both
-- pickup and drop OTP. Unlike every other platform_settings column, this one
-- deliberately has NO hard ceiling enforced in PlatformSettingsServiceImpl --
-- whatever Admin sets is used as-is. Default 120 (2 hours), up from the old
-- 15-minute hardcoded value.
-- ============================================================

ALTER TABLE platform_settings
    ADD COLUMN IF NOT EXISTS otp_expiry_minutes INTEGER NOT NULL DEFAULT 120;

-- ============================================================
-- VegGo Fresh Platform — Flyway Migration V48
-- Customer Module: Add email + avatar_public_id to customer_profiles
--
-- email             — local, editable, optional copy (same pattern as
--                      Vendor's Shop.email / Delivery's DeliveryPartnerProfile.email).
--                      There is no cross-module write path into Auth's User entity,
--                      so email is now sourced/edited here instead of via
--                      UserLookupService (which remains read-only, used for phone only).
-- avatar_public_id  — Cloudinary public_id for the current avatar, needed to delete
--                      the old asset when the avatar is replaced. Never returned to
--                      API clients.
-- ============================================================

ALTER TABLE customer_profiles
    ADD COLUMN IF NOT EXISTS email             VARCHAR(255) NULL,
    ADD COLUMN IF NOT EXISTS avatar_public_id  VARCHAR(500) NULL;

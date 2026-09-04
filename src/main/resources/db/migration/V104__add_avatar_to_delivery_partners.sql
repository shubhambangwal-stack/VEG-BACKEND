-- ============================================================
-- VegGo Fresh Platform — Flyway Migration V104
-- Delivery Module: Add avatar_url + avatar_public_id to delivery_partners
--
-- delivery_partners had no personal-photo field at all before this. Same
-- optional/single/auto-delete-on-replace pattern as Customer's avatar and
-- Vendor's profile/store images. Set via PUT /api/delivery/account-settings.
-- ============================================================

ALTER TABLE delivery_partners
    ADD COLUMN IF NOT EXISTS avatar_url         TEXT         NULL,
    ADD COLUMN IF NOT EXISTS avatar_public_id   VARCHAR(500) NULL;

-- ============================================================
-- VegGo Fresh — Vendor Module (V70-V89 range)
-- V76: Store Profile + Account Settings fields on vendor_shops
-- ============================================================

ALTER TABLE vendor_shops
    ADD COLUMN store_image_url TEXT NULL,
    ADD COLUMN store_bio TEXT NULL,
    ADD COLUMN store_attributes TEXT NULL,
    ADD COLUMN profile_image_url TEXT NULL,
    ADD COLUMN business_license_number VARCHAR(100) NULL,
    ADD COLUMN new_order_alerts_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN low_stock_notifications_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN payout_confirmations_enabled BOOLEAN NOT NULL DEFAULT FALSE;

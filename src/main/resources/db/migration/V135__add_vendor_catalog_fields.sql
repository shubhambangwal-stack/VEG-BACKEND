-- ============================================================
-- VegGo Fresh Platform — Flyway Migration V44
-- Vendor Module: Add additional fields to products & categories for Customer Browse
-- ============================================================

ALTER TABLE vendor_categories
    ADD COLUMN IF NOT EXISTS icon_url TEXT NULL;

ALTER TABLE vendor_products
    ADD COLUMN IF NOT EXISTS unit             VARCHAR(50)  NULL,
    ADD COLUMN IF NOT EXISTS is_best_seller   BOOLEAN      NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS discount_percent INT          NULL,
    ADD COLUMN IF NOT EXISTS badge            VARCHAR(50)  NULL,
    ADD COLUMN IF NOT EXISTS why_its_great    TEXT         NULL,
    ADD COLUMN IF NOT EXISTS storage_tips     TEXT         NULL;

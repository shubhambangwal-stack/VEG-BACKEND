-- ============================================================
-- VegGo Fresh Platform — Flyway Migration V44
-- Vendor Module: Add additional fields to products & categories for Customer Browse
-- ============================================================

ALTER TABLE vendor_categories
    ADD COLUMN icon_url TEXT NULL;

ALTER TABLE vendor_products
    ADD COLUMN unit             VARCHAR(50)  NULL,
    ADD COLUMN is_best_seller   BOOLEAN      NOT NULL DEFAULT FALSE,
    ADD COLUMN discount_percent INT          NULL,
    ADD COLUMN badge            VARCHAR(50)  NULL,
    ADD COLUMN why_its_great    TEXT         NULL,
    ADD COLUMN storage_tips     TEXT         NULL;

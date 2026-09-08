-- ============================================================
-- VegGo Fresh Platform — Flyway Migration V125
-- Admin Module: Add image_url + image_public_id to catalog_subcategories
--
-- Subcategories had no image field at all before this. Same optional/single/
-- auto-delete-on-replace pattern as the category image above.
-- ============================================================

ALTER TABLE catalog_subcategories
    ADD COLUMN IF NOT EXISTS image_url        VARCHAR(500) NULL,
    ADD COLUMN IF NOT EXISTS image_public_id  VARCHAR(500) NULL;

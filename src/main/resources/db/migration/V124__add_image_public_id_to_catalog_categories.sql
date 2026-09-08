-- ============================================================
-- VegGo Fresh Platform — Flyway Migration V124
-- Admin Module: Add image_public_id to catalog_categories
--
-- catalog_categories.image_url already existed (V111) as a plain string field
-- with no upload endpoint. Category image is now a real Cloudinary upload
-- (optional, single, auto-delete-on-replace) -- this column stores the
-- public_id needed to delete the old asset. Never returned on CategoryResponseDto.
-- ============================================================

ALTER TABLE catalog_categories
    ADD COLUMN IF NOT EXISTS image_public_id VARCHAR(500) NULL;

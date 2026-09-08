-- ============================================================
-- VegGo Fresh Platform — Flyway Migration V81
-- Vendor Module: Add image public_id columns to vendor_shops
--
-- store_image_public_id   — Cloudinary public_id for store_image_url
-- profile_image_public_id — Cloudinary public_id for profile_image_url (owner's photo)
-- Both required to delete the old asset from Cloudinary when the image is replaced.
-- Never returned on any response DTO.
-- ============================================================

ALTER TABLE vendor_shops
    ADD COLUMN IF NOT EXISTS store_image_public_id    VARCHAR(500) NULL,
    ADD COLUMN IF NOT EXISTS profile_image_public_id  VARCHAR(500) NULL;

-- ============================================================
-- VegGo Fresh Platform — Flyway Migration V40
-- Customer Module: Add fullName and avatarUrl to customer_profiles
-- ============================================================

ALTER TABLE customer_profiles
    ADD COLUMN full_name  VARCHAR(100) NULL,
    ADD COLUMN avatar_url TEXT         NULL;

-- ============================================================
-- VegGo Fresh Platform — Flyway Migration V41
-- Customer Module: Add label column to addresses table
-- ============================================================

ALTER TABLE addresses
    ADD COLUMN label VARCHAR(50) NULL;

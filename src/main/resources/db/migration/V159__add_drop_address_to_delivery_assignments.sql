-- ============================================================
-- VegGo Fresh Platform — Flyway Migration V158
-- Delivery Module: Add drop_address column to delivery_assignments
-- (previously only drop_latitude/drop_longitude were stored -- a
-- delivery partner had no human-readable customer address, only
-- coordinates)
-- ============================================================

ALTER TABLE delivery_assignments ADD COLUMN IF NOT EXISTS drop_address VARCHAR(500) NULL;

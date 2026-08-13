-- ============================================================
-- VegGo Fresh Platform — Flyway Migration V112
-- Admin Module: Add quantity unit/value fields to catalog_products
-- ============================================================

ALTER TABLE catalog_products
ADD COLUMN quantity_unit VARCHAR(50) NOT NULL DEFAULT 'kg',
ADD COLUMN quantity_value DECIMAL(10,2) NOT NULL DEFAULT 1.00;

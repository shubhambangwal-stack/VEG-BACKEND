-- ============================================================
-- VegGo Fresh Platform — Flyway Migration V6
-- Auth Module: Add password column to users table
-- ============================================================

ALTER TABLE users ADD COLUMN password VARCHAR(255) NULL;

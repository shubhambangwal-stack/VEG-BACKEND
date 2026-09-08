-- ============================================================
-- VegGo Fresh â€” Notification Module Reconcile for PostgreSQL
-- V152: reconcile `notifications` table to the current
-- entity schema (PostgreSQL, idempotent).
--
-- WHY THIS EXISTS:
-- Similar to V151 for MySQL, some environments already ran an older V150
-- that lacked columns like `body`, `data`, `recipient_role`.
-- This script adds the missing columns idempotently for PostgreSQL.
-- ============================================================

ALTER TABLE notifications ADD COLUMN IF NOT EXISTS body TEXT;
ALTER TABLE notifications ADD COLUMN IF NOT EXISTS data TEXT;
ALTER TABLE notifications ADD COLUMN IF NOT EXISTS recipient_role VARCHAR(20) DEFAULT 'CUSTOMER';
ALTER TABLE notifications ADD COLUMN IF NOT EXISTS type VARCHAR(50) DEFAULT 'SYSTEM';
ALTER TABLE notifications ADD COLUMN IF NOT EXISTS is_read BOOLEAN DEFAULT FALSE;
ALTER TABLE notifications ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0;

-- Optionally, drop defaults if they shouldn't exist permanently, 
-- but keeping them is safe since Hibernate dictates constraints.

-- ============================================================
-- VegGo Fresh — Notification Module (V150-V169 range)
-- V151: reconcile prod `notifications` table to the current
-- entity schema (MySQQL, idempotent).
--
-- WHY THIS EXISTS
--   An earlier commit shipped a DIFFERENT V150
--   (V150__create_notification_tables.sql: recipient_type /
--   message / payload / status … , NO `body`). Because prod runs
--   flyway.validate-on-migrate: false, Flyway saw version 150 as
--   "already applied" and silently SKIPPED the new
--   V150__notifications.sql — the table was never upgraded, and
--   ddl-auto: validate then aborted: "missing column [body] in
--   table [notifications]".
--
--   This migration makes the table match the entity exactly,
--   column-by-column, WITHOUT failing on databases where V150 was
--   applied correctly. MySQL has no "ADD COLUMN IF NOT EXISTS",
--   so each column uses an information_schema guard via prepared
--   statements. Every statement is a no-op when the object already
--   exists, which makes the same file safe on fresh databases too.
--
--   Target engine: MySQL (the PROD profile). Local/dev run
--   Flyway-off + PostgreSQL and are unaffected.
-- ============================================================

-- ── Helper: one prepared-statement block per possibly-missing column ──

-- body (added in the current V150; absent in the legacy V150)
SET @missing := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'notifications' AND COLUMN_NAME = 'body');
SET @ddl := IF(@missing = 0, 'SELECT 1',
    'ALTER TABLE notifications ADD COLUMN body TEXT NULL');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- data (legacy schema called it `payload`)
SET @missing := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'notifications' AND COLUMN_NAME = 'data');
SET @ddl := IF(@missing = 0, 'SELECT 1',
    'ALTER TABLE notifications ADD COLUMN data TEXT NULL');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- recipient_role (legacy schema used `recipient_type`)
SET @missing := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'notifications' AND COLUMN_NAME = 'recipient_role');
SET @ddl := IF(@missing = 0, 'SELECT 1',
    'ALTER TABLE notifications ADD COLUMN recipient_role VARCHAR(20) NOT NULL');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- type (legacy schema used `notification_type`)
SET @missing := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'notifications' AND COLUMN_NAME = 'type');
SET @ddl := IF(@missing = 0, 'SELECT 1',
    'ALTER TABLE notifications ADD COLUMN type VARCHAR(50) NOT NULL');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- is_read (legacy schema tracked `read_at` instead)
SET @missing := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'notifications' AND COLUMN_NAME = 'is_read');
SET @ddl := IF(@missing = 0, 'SELECT 1',
    'ALTER TABLE notifications ADD COLUMN is_read BOOLEAN NOT NULL DEFAULT FALSE');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- version (BaseEntity optimistic-lock column; absent in legacy schema)
SET @missing := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'notifications' AND COLUMN_NAME = 'version');
SET @ddl := IF(@missing = 0, 'SELECT 1',
    'ALTER TABLE notifications ADD COLUMN version BIGINT NOT NULL DEFAULT 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ── Indexes used by the repository queries (idempotent) ────────────────

-- Newest-first inbox + unread-count covering index.
SET @missing := (SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'notifications'
      AND INDEX_NAME = 'idx_notifications_recipient_created');
SET @ddl := IF(@missing = 0, 'SELECT 1',
    'CREATE INDEX idx_notifications_recipient_created ON notifications(recipient_id, created_at)');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Unread-count lookup.
SET @missing := (SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'notifications'
      AND INDEX_NAME = 'idx_notifications_recipient_read');
SET @ddl := IF(@missing = 0, 'SELECT 1',
    'CREATE INDEX idx_notifications_recipient_read ON notifications(recipient_id, is_read)');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Drop the legacy per-query helper variables.
SET @missing := NULL;
SET @ddl := NULL;
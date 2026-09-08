-- ============================================================
-- VegGo Fresh — Notification Module Reconcile for PostgreSQL
-- V154: Drop NOT NULL constraints from all remaining legacy columns
--
-- WHY THIS EXISTS:
-- V153 handled `recipient_type` and a few others, but there are
-- more legacy columns like `message`, `payload`, `priority`, etc.
-- This script removes NOT NULL constraints from ALL of them 
-- so Hibernate inserts (which don't populate them) succeed.
-- ============================================================

DO $$
DECLARE
    colname text;
    legacy_columns text[] := ARRAY['message', 'payload', 'read_at', 'priority', 'channel', 'action_url'];
BEGIN
    FOREACH colname IN ARRAY legacy_columns
    LOOP
        IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='notifications' AND column_name=colname) THEN
            EXECUTE format('ALTER TABLE notifications ALTER COLUMN %I DROP NOT NULL', colname);
        END IF;
    END LOOP;
END $$;

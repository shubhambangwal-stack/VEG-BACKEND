-- ============================================================
-- VegGo Fresh — Notification Module Reconcile for PostgreSQL
-- V153: Drop NOT NULL constraints from legacy columns
--
-- WHY THIS EXISTS:
-- The entity schema changed in V150/V152, replacing columns like
-- `recipient_type` with `recipient_role`. Legacy environments 
-- that have these old columns still have NOT NULL constraints
-- on them, causing Hibernate inserts to fail.
-- ============================================================

ALTER TABLE notifications ALTER COLUMN recipient_type DROP NOT NULL;

-- If there are other legacy columns with NOT NULL constraints, drop them too
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='notifications' AND column_name='notification_type') THEN
        ALTER TABLE notifications ALTER COLUMN notification_type DROP NOT NULL;
    END IF;
    
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='notifications' AND column_name='status') THEN
        ALTER TABLE notifications ALTER COLUMN status DROP NOT NULL;
    END IF;
END $$;

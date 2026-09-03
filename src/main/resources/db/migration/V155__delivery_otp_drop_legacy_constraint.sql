-- ============================================================
-- VegGo Fresh — Delivery OTP Constraint Fix for PostgreSQL
-- V155: Drop legacy unique constraint on assignment_id
--
-- WHY THIS EXISTS:
-- In V103, an attempt was made to drop the constraint on 
-- delivery_otps(assignment_id) so multiple OTPs could exist 
-- (PICKUP and DROP). However, the constraint name was misspelled
-- as `uk_delivery_otps_assignment_id` instead of the original
-- `uk_delivery_otps_assignment` from V93. 
--
-- This script safely drops the old correctly named constraint.
-- ============================================================

DO $$
BEGIN
    ALTER TABLE delivery_otps DROP CONSTRAINT IF EXISTS uk_delivery_otps_assignment;
EXCEPTION
    WHEN undefined_table THEN
        NULL;
END $$;

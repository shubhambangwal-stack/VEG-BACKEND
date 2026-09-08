-- ═══════════════════════════════════════════════════════════════════════
-- Delivery V90-V109 range -- V99 was the highest applied before this round,
-- so this is V100. Confirm that still holds against your actual DB before
-- applying (another migration may have landed here since this was written).
-- ═══════════════════════════════════════════════════════════════════════
-- ═══════════════════════════════════════════════════════════════════════

-- 1) delivery_otps: add `type` (PICKUP/DROP), move unique constraint from
--    assignment_id alone to (assignment_id, type) -- one assignment can now
--    legitimately have two OTP rows.
ALTER TABLE delivery_otps ADD COLUMN type VARCHAR(10);

-- Backfill existing rows -- everything that existed before this migration
-- was a drop OTP (pickup OTP didn't exist as a concept yet).
UPDATE delivery_otps SET type = 'DROP' WHERE type IS NULL;

ALTER TABLE delivery_otps ALTER COLUMN type SET NOT NULL;

-- Drop the old single-column unique constraint if one exists (name may vary
-- by how it was originally created -- check \d delivery_otps first).
ALTER TABLE delivery_otps DROP CONSTRAINT IF EXISTS uk_delivery_otps_assignment_id;
ALTER TABLE delivery_otps DROP CONSTRAINT IF EXISTS delivery_otps_assignment_id_key;

CREATE UNIQUE INDEX uk_delivery_otps_assignment_id_type
    ON delivery_otps(assignment_id, type)
    WHERE deleted_at IS NULL;

-- 2) delivery_partners: new column for KYC rejection reason (didn't exist --
--    DeliveryTestController only ever supported approve, never reject).
ALTER TABLE delivery_partners ADD COLUMN rejection_reason VARCHAR(500);

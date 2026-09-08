-- ============================================================
-- VegGo Fresh Platform — Flyway Migration V106
-- Delivery Module: Add public_id to delivery_proof_of_delivery
--
-- Required to delete the old Cloudinary asset if a proof-of-delivery photo is
-- ever resubmitted for the same assignment.
-- ============================================================

ALTER TABLE delivery_proof_of_delivery
    ADD COLUMN IF NOT EXISTS public_id VARCHAR(500) NULL;

-- ============================================================
-- VegGo Fresh Platform — Flyway Migration V105
-- Delivery Module: Add public_id to delivery_documents
--
-- Required to delete the old Cloudinary asset when a document type is
-- re-uploaded (KYC vault re-upload, or onboarding Step 1/2 license/insurance
-- photos, which write into this same table).
-- ============================================================

ALTER TABLE delivery_documents
    ADD COLUMN IF NOT EXISTS public_id VARCHAR(500) NULL;

-- ============================================================
-- VegGo Fresh Platform — Flyway Migration V82
-- Vendor Module: Add public_id to vendor_documents
--
-- Required to delete the old Cloudinary asset when a document is re-uploaded
-- for the same document type (BUSINESS_LICENSE, TAX_ID, GOVERNMENT_ID).
-- Never returned on VendorDocumentResponseDto.
-- ============================================================

ALTER TABLE vendor_documents
    ADD COLUMN IF NOT EXISTS public_id VARCHAR(500) NULL;

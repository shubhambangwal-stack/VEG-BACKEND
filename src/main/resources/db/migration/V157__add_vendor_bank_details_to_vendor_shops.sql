-- ============================================================
-- VegGo Fresh Platform — Flyway Migration V157
-- Vendor Module: Add bank details columns to vendor_shops
-- ============================================================

ALTER TABLE vendor_shops ADD COLUMN IF NOT EXISTS bank_name VARCHAR(100) NULL;
ALTER TABLE vendor_shops ADD COLUMN IF NOT EXISTS account_holder_name VARCHAR(255) NULL;
ALTER TABLE vendor_shops ADD COLUMN IF NOT EXISTS account_number VARCHAR(50) NULL;
ALTER TABLE vendor_shops ADD COLUMN IF NOT EXISTS ifsc_code VARCHAR(20) NULL;
ALTER TABLE vendor_shops ADD COLUMN IF NOT EXISTS agreed_to_payout_terms BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE vendor_shops ADD COLUMN IF NOT EXISTS has_bank_details BOOLEAN NOT NULL DEFAULT FALSE;

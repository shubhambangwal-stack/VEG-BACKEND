-- ============================================================
-- VegGo Fresh — Delivery Module (V90-V109 range)
-- V98: Onboarding fields on delivery_partners
--
-- Bank fields (bank_name, account_holder_name, account_number, ifsc_code) are
-- FLAGGED: this data belongs to Payment module long-term. Stored here for now
-- per team decision. account_number is PLAIN TEXT -- must be encrypted at
-- rest before any production use. See NOTES.md.
-- ============================================================

ALTER TABLE delivery_partners
    ADD COLUMN city_of_operation VARCHAR(100) NULL,
    ADD COLUMN license_number VARCHAR(100) NULL,
    ADD COLUMN plate_number VARCHAR(50) NULL,
    ADD COLUMN vehicle_model VARCHAR(100) NULL,
    ADD COLUMN manufacture_year INT NULL,
    ADD COLUMN bank_name VARCHAR(100) NULL,
    ADD COLUMN account_holder_name VARCHAR(255) NULL,
    ADD COLUMN account_number VARCHAR(50) NULL,
    ADD COLUMN ifsc_code VARCHAR(20) NULL,
    ADD COLUMN agreed_to_payout_terms BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN has_basic_info BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN verification_step INT NOT NULL DEFAULT 0;

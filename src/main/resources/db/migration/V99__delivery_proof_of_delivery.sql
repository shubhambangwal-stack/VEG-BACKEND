-- ============================================================
-- VegGo Fresh — Delivery Module (V90-V109 range)
-- V99: Proof of Delivery Table (photo + checklist)
--
-- Runs ALONGSIDE the existing delivery_otps flow, not instead of it -- both
-- are required to complete a delivery. photo_url is MOCKED storage (see
-- MockFileStorageService). photo is required; checklist booleans + notes
-- are optional/best-effort.
-- ============================================================

CREATE TABLE delivery_proof_of_delivery (
    id UUID NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    deleted_at TIMESTAMP(6) NULL,
    version BIGINT NOT NULL DEFAULT 0,
    assignment_id UUID NOT NULL,
    photo_url VARCHAR(500),
    delivered_to_customer_directly BOOLEAN NOT NULL DEFAULT FALSE,
    left_at_front_door BOOLEAN NOT NULL DEFAULT FALSE,
    packaging_intact BOOLEAN NOT NULL DEFAULT FALSE,
    address_verified_manually BOOLEAN NOT NULL DEFAULT FALSE,
    notes VARCHAR(1000),
    PRIMARY KEY (id),
    CONSTRAINT uk_delivery_proof_assignment UNIQUE (assignment_id),
    CONSTRAINT fk_delivery_proof_assignment FOREIGN KEY (assignment_id) REFERENCES delivery_assignments(id)
);

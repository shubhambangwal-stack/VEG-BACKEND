-- ============================================================
-- VegGo Fresh Platform — Flyway Migration V70
-- Vendor Module: Shop Table
-- ============================================================

CREATE TABLE vendor_shops (
    id UUID NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    deleted_at TIMESTAMP(6) NULL,
    version BIGINT NOT NULL DEFAULT 0,
    owner_user_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    address TEXT,
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    kyc_status VARCHAR(50) NOT NULL,
    is_online BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id),
    CONSTRAINT fk_vendor_shops_owner FOREIGN KEY (owner_user_id) REFERENCES users(id)
);

CREATE INDEX idx_vendor_shops_owner ON vendor_shops(owner_user_id);
CREATE INDEX idx_vendor_shops_kyc_status ON vendor_shops(kyc_status);

-- ============================================================
-- VegGo Fresh — Delivery Module (V90-V109 range)
-- V90: Delivery Partners Table
-- ============================================================

CREATE TABLE delivery_partners (
    id UUID NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    deleted_at TIMESTAMP(6) NULL,
    version BIGINT NOT NULL DEFAULT 0,
    user_id UUID NOT NULL,
    kyc_status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    is_online BOOLEAN NOT NULL DEFAULT FALSE,
    current_latitude DOUBLE PRECISION,
    current_longitude DOUBLE PRECISION,
    vehicle_type VARCHAR(50),
    PRIMARY KEY (id),
    CONSTRAINT uk_delivery_partners_user UNIQUE (user_id),
    CONSTRAINT fk_delivery_partners_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX idx_delivery_partners_online_kyc ON delivery_partners(is_online, kyc_status);

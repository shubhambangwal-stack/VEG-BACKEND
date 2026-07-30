-- ============================================================
-- VegGo Fresh — Delivery Module (V90-V109 range)
-- V95: Delivery Documents Table (KYC vault, per-document status)
-- ============================================================

CREATE TABLE delivery_documents (
    id UUID NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    deleted_at TIMESTAMP(6) NULL,
    version BIGINT NOT NULL DEFAULT 0,
    delivery_partner_user_id UUID NOT NULL,
    document_type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    file_url VARCHAR(500),
    expiry_date DATE,
    PRIMARY KEY (id),
    CONSTRAINT uk_delivery_documents_partner_type UNIQUE (delivery_partner_user_id, document_type),
    CONSTRAINT fk_delivery_documents_partner FOREIGN KEY (delivery_partner_user_id) REFERENCES users(id)
);

CREATE INDEX idx_delivery_documents_partner ON delivery_documents(delivery_partner_user_id);

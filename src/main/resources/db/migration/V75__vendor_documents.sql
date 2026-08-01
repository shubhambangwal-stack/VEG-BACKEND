-- ============================================================
-- VegGo Fresh — Vendor Module (V70-V89 range)
-- V75: Vendor Documents Table (KYC vault, per-document status)
-- ============================================================

CREATE TABLE vendor_documents (
    id UUID NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    deleted_at TIMESTAMP(6) NULL,
    version BIGINT NOT NULL DEFAULT 0,
    shop_id UUID NOT NULL,
    document_type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    file_url VARCHAR(500),
    PRIMARY KEY (id),
    CONSTRAINT uk_vendor_documents_shop_type UNIQUE (shop_id, document_type),
    CONSTRAINT fk_vendor_documents_shop FOREIGN KEY (shop_id) REFERENCES vendor_shops(id)
);

CREATE INDEX idx_vendor_documents_shop ON vendor_documents(shop_id);

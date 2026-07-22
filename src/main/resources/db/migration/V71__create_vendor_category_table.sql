-- ============================================================
-- VegGo Fresh Platform — Flyway Migration V71
-- Vendor Module: Category Table
-- ============================================================

CREATE TABLE vendor_categories (
    id UUID NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    deleted_at TIMESTAMP(6) NULL,
    version BIGINT NOT NULL DEFAULT 0,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id)
);

CREATE INDEX idx_vendor_categories_active ON vendor_categories(is_active);

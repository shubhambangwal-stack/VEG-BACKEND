-- ============================================================
-- VegGo Fresh — Vendor Module (V70-V89 range)
-- V78: Vendor Special Closures Table (holidays, planned closures)
-- ============================================================

CREATE TABLE vendor_special_closures (
    id UUID NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    deleted_at TIMESTAMP(6) NULL,
    version BIGINT NOT NULL DEFAULT 0,
    shop_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_vendor_special_closures_shop FOREIGN KEY (shop_id) REFERENCES vendor_shops(id)
);

CREATE INDEX idx_vendor_special_closures_shop ON vendor_special_closures(shop_id);

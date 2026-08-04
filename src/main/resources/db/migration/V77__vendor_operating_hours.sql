-- ============================================================
-- VegGo Fresh — Vendor Module (V70-V89 range)
-- V77: Vendor Operating Hours Table (one row per day of week per shop)
-- ============================================================

CREATE TABLE vendor_operating_hours (
    id UUID NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    deleted_at TIMESTAMP(6) NULL,
    version BIGINT NOT NULL DEFAULT 0,
    shop_id UUID NOT NULL,
    day_of_week VARCHAR(20) NOT NULL,
    is_open BOOLEAN NOT NULL DEFAULT TRUE,
    open_time TIME NULL,
    close_time TIME NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_vendor_operating_hours_shop_day UNIQUE (shop_id, day_of_week),
    CONSTRAINT fk_vendor_operating_hours_shop FOREIGN KEY (shop_id) REFERENCES vendor_shops(id)
);

CREATE INDEX idx_vendor_operating_hours_shop ON vendor_operating_hours(shop_id);

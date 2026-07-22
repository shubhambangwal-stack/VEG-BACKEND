-- ============================================================
-- VegGo Fresh Platform — Flyway Migration V73
-- Vendor Module: Inventory Item Table
-- ============================================================

CREATE TABLE vendor_inventory_items (
    id UUID NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    deleted_at TIMESTAMP(6) NULL,
    version BIGINT NOT NULL DEFAULT 0,
    product_id UUID NOT NULL,
    stock_quantity INT NOT NULL DEFAULT 0,
    low_stock_threshold INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_vendor_inventory_product UNIQUE (product_id),
    CONSTRAINT fk_vendor_inventory_product FOREIGN KEY (product_id) REFERENCES vendor_products(id)
);

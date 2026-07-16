-- ============================================================
-- VegGo Fresh Platform — Flyway Migration V72
-- Vendor Module: Product Table
-- ============================================================

CREATE TABLE vendor_products (
    id UUID NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    deleted_at TIMESTAMP(6) NULL,
    version BIGINT NOT NULL DEFAULT 0,
    shop_id UUID NOT NULL,
    category_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    price DECIMAL(10, 2) NOT NULL,
    image_url TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id),
    CONSTRAINT fk_vendor_products_shop FOREIGN KEY (shop_id) REFERENCES vendor_shops(id),
    CONSTRAINT fk_vendor_products_category FOREIGN KEY (category_id) REFERENCES vendor_categories(id)
);

CREATE INDEX idx_vendor_products_shop ON vendor_products(shop_id);
CREATE INDEX idx_vendor_products_category ON vendor_products(category_id);
CREATE INDEX idx_vendor_products_active ON vendor_products(is_active);

-- ============================================================
-- VegGo Fresh — Vendor Module (V70-V89 range)
-- V79: Vendor Shop Ratings Table
--
-- Scoped to (order_id, shop_id) rather than just order_id -- a single order can
-- span multiple vendors, so each shop in a multi-vendor order is rated
-- independently. See VendorRatingService for the cross-module write contract --
-- not wired to anything yet, same situation as DeliveryPartnerRating. See NOTES_VENDOR.md.
-- ============================================================

CREATE TABLE vendor_shop_ratings (
    id UUID NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    deleted_at TIMESTAMP(6) NULL,
    version BIGINT NOT NULL DEFAULT 0,
    order_id UUID NOT NULL,
    shop_id UUID NOT NULL,
    customer_user_id UUID NOT NULL,
    rating_value INT NOT NULL,
    comment VARCHAR(1000),
    PRIMARY KEY (id),
    CONSTRAINT uk_vendor_shop_ratings_order_shop UNIQUE (order_id, shop_id),
    CONSTRAINT fk_vendor_shop_ratings_shop FOREIGN KEY (shop_id) REFERENCES vendor_shops(id),
    CONSTRAINT fk_vendor_shop_ratings_customer FOREIGN KEY (customer_user_id) REFERENCES users(id)
);

CREATE INDEX idx_vendor_shop_ratings_shop ON vendor_shop_ratings(shop_id);

-- ═══════════════════════════════════════════════════════════════════════
-- Catalog pivot: Vendor no longer owns products/categories. This table is
-- the bridge between a Shop and Admin's master CatalogProduct (referenced
-- by id only — no FK constraint into admin's schema on purpose, since
-- cross-module FK constraints create a hard coupling this codebase
-- otherwise avoids at the code level too).
--
-- Legacy vendor_products / vendor_categories / vendor_inventory_items
-- tables are deliberately LEFT IN PLACE, not dropped, per team decision —
-- old data isn't migrated this round.
-- ═══════════════════════════════════════════════════════════════════════

CREATE TABLE vendor_listings (
    id                  UUID PRIMARY KEY,
    shop_id             UUID NOT NULL REFERENCES vendor_shops(id),
    catalog_product_id  UUID NOT NULL,
    is_listed           BOOLEAN NOT NULL DEFAULT FALSE,
    created_at          TIMESTAMP NOT NULL,
    updated_at          TIMESTAMP,
    deleted_at          TIMESTAMP
);

CREATE UNIQUE INDEX uk_vendor_listings_shop_catalog_product
    ON vendor_listings(shop_id, catalog_product_id)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_vendor_listings_catalog_product_listed
    ON vendor_listings(catalog_product_id)
    WHERE is_listed = TRUE AND deleted_at IS NULL;

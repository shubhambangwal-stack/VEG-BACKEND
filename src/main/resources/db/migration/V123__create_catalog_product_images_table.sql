-- ============================================================
-- VegGo Fresh Platform — Flyway Migration V123
-- Admin Module: Product multi-image support (catalog_product_images)
--
-- Replaces the old single catalog_products.image_url as the source of truth
-- for product photos. A product can now have an unlimited number of images;
-- at least 1 is required (enforced in AdminProductServiceImpl, not here).
-- sort_order supports full drag-to-reorder -- position 0 is the cover image
-- shown wherever a single thumbnail is needed (ProductResponseDto.imageUrl
-- keeps returning that cover image for backward compatibility with existing
-- consumers; ProductResponseDto.imageUrls returns the full ordered gallery).
--
-- catalog_products.image_url itself is left in place, untouched, for now --
-- see the note in V123 companion doc / PR description. It is no longer
-- written to by AdminProductServiceImpl.
-- ============================================================

CREATE TABLE catalog_product_images (
    id           UUID          NOT NULL PRIMARY KEY,
    created_at   TIMESTAMP(6)  NOT NULL,
    updated_at   TIMESTAMP(6)  NOT NULL,
    deleted_at   TIMESTAMP(6)  NULL,
    version      BIGINT        NOT NULL DEFAULT 0,

    product_id   UUID          NOT NULL,
    image_url    VARCHAR(500)  NOT NULL,
    public_id    VARCHAR(500)  NULL,
    sort_order   INT           NOT NULL DEFAULT 0,

    CONSTRAINT fk_product_image_product FOREIGN KEY (product_id)
        REFERENCES catalog_products(id)
);

CREATE INDEX idx_product_image_product_id ON catalog_product_images(product_id);

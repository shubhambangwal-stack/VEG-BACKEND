-- ============================================================
-- VegGo Fresh Platform — Flyway Migration V111
-- Admin Module: Master Product Catalog (Phase 1)
-- ============================================================
-- Tables prefixed catalog_* deliberately, to avoid colliding with
-- Vendor's pre-pivot `products` table (V72). Vendor's table is not
-- touched by this migration — that rework is a separate, later step.

CREATE TABLE catalog_categories (
                                    id           UUID     NOT NULL PRIMARY KEY,
                                    created_at   TIMESTAMP(6)  NOT NULL,
                                    updated_at   TIMESTAMP(6)  NOT NULL,
                                    deleted_at   TIMESTAMP(6)  NULL,
                                    version      BIGINT       NOT NULL DEFAULT 0,

                                    name          VARCHAR(150)  NOT NULL,
                                    description   VARCHAR(1000) NULL,
                                    image_url     VARCHAR(500)  NULL,
                                    display_order INT           NOT NULL DEFAULT 0,
                                    is_active     BOOLEAN       NOT NULL DEFAULT TRUE,

                                    CONSTRAINT uq_catalog_category_name UNIQUE (name)
);

CREATE TABLE catalog_subcategories (
                                       id           UUID     NOT NULL PRIMARY KEY,
                                       created_at   TIMESTAMP(6)  NOT NULL,
                                       updated_at   TIMESTAMP(6)  NOT NULL,
                                       deleted_at   TIMESTAMP(6)  NULL,
                                       version      BIGINT       NOT NULL DEFAULT 0,

                                       category_id   UUID      NOT NULL,
                                       name          VARCHAR(150)  NOT NULL,
                                       display_order INT           NOT NULL DEFAULT 0,
                                       is_active     BOOLEAN       NOT NULL DEFAULT TRUE,

                                       CONSTRAINT fk_subcategory_category FOREIGN KEY (category_id)
                                           REFERENCES catalog_categories(id),
                                       CONSTRAINT uq_subcategory_name_per_category UNIQUE (category_id, name)
);

CREATE INDEX idx_subcategory_category_id ON catalog_subcategories(category_id);

CREATE TABLE catalog_products (
                                  id           UUID     NOT NULL PRIMARY KEY,
                                  created_at   TIMESTAMP(6)  NOT NULL,
                                  updated_at   TIMESTAMP(6)  NOT NULL,
                                  deleted_at   TIMESTAMP(6)  NULL,
                                  version      BIGINT       NOT NULL DEFAULT 0,

                                  name           VARCHAR(200)   NOT NULL,
                                  description    VARCHAR(2000)  NULL,
                                  category_id    UUID       NOT NULL,
                                  subcategory_id UUID       NOT NULL,
                                  price          DECIMAL(10,2)  NOT NULL,
                                  image_url      VARCHAR(500)   NULL,
                                  is_active      BOOLEAN        NOT NULL DEFAULT TRUE,

                                  CONSTRAINT fk_product_category FOREIGN KEY (category_id)
                                      REFERENCES catalog_categories(id),
                                  CONSTRAINT fk_product_subcategory FOREIGN KEY (subcategory_id)
                                      REFERENCES catalog_subcategories(id),
                                  CONSTRAINT uq_product_name_per_subcategory UNIQUE (subcategory_id, name)
);

CREATE INDEX idx_product_category_id ON catalog_products(category_id);
CREATE INDEX idx_product_subcategory_id ON catalog_products(subcategory_id);
CREATE INDEX idx_product_name ON catalog_products(name);

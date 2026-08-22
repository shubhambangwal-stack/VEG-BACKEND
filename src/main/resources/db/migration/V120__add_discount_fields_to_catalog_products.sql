-- Adds the discount/pack-size fields introduced on CatalogProduct this round.
-- Both nullable: original_price null = no active discount (see
-- AdminProductServiceImpl.computeDiscountPercent()); unit null = not yet set
-- by Admin on older rows created before this field existed.
ALTER TABLE catalog_products
    ADD COLUMN original_price NUMERIC(10, 2),
    ADD COLUMN unit VARCHAR(100);
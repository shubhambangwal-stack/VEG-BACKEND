-- ============================================================
-- VegGo Fresh — Delivery Module (V90-V109 range)
-- V96: Contact-resolution + shop-snapshot fields on delivery_assignments
--
-- customer_user_id / shop_owner_user_id let Delivery resolve LIVE phone
-- numbers via UserLookupService at read time.
-- shop_name / shop_address are a SNAPSHOT taken at dispatch time -- Vendor
-- module has no ShopLookupService yet to resolve this live. See NOTES.md.
-- ============================================================

ALTER TABLE delivery_assignments
    ADD COLUMN customer_user_id UUID NULL,
    ADD COLUMN shop_owner_user_id UUID NULL,
    ADD COLUMN shop_name VARCHAR(255) NULL,
    ADD COLUMN shop_address VARCHAR(500) NULL;

ALTER TABLE delivery_assignments
    ADD CONSTRAINT fk_delivery_assignments_customer FOREIGN KEY (customer_user_id) REFERENCES users(id),
    ADD CONSTRAINT fk_delivery_assignments_shop_owner FOREIGN KEY (shop_owner_user_id) REFERENCES users(id);

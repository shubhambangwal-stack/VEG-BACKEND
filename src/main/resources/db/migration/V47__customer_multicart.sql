-- ═══════════════════════════════════════════════════════════════════════
-- Multi-cart model support (PROJECT_STATE NEW ARCHITECTURE §2)
--
-- IMPORTANT: rename this file to the next available V-number in your
-- Customer module's Flyway range (V40–V69) before dropping it in, e.g.
-- V4X__customer_multicart.sql -> V47__customer_multicart.sql. Do not run
-- as-is; check your actual highest-applied V number first.
-- ═══════════════════════════════════════════════════════════════════════

-- 1) Cart is no longer one-per-customer.
--    The unique constraint name below is a guess (Hibernate's default
--    naming). CONFIRM THE REAL CONSTRAINT NAME on your DB before running,
--    e.g. in psql: \d carts   (look for a UNIQUE constraint on user_id).
ALTER TABLE carts DROP CONSTRAINT IF EXISTS uk_carts_user_id;
ALTER TABLE carts DROP CONSTRAINT IF EXISTS carts_user_id_key; -- Postgres' auto-generated name is often this instead

-- 2) Candidate-vendor set per cart (narrowed to the intersection as items are added).
CREATE TABLE IF NOT EXISTS cart_candidate_vendors (
    cart_id   UUID NOT NULL REFERENCES carts(id),
    vendor_id UUID NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_cart_candidate_vendors_cart_id ON cart_candidate_vendors(cart_id);

-- 3) Order: multi-cart / vendor-broadcast prep (§2-3).
ALTER TABLE orders ADD COLUMN IF NOT EXISTS source_cart_id UUID;

CREATE TABLE IF NOT EXISTS order_candidate_vendors (
    order_id  UUID NOT NULL REFERENCES orders(id),
    vendor_id UUID NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_order_candidate_vendors_order_id ON order_candidate_vendors(order_id);

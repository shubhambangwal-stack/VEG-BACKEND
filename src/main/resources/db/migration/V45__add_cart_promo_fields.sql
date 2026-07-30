-- ============================================================
-- VegGo Fresh Platform — Flyway Migration V45
-- Customer Module: Add promo fields to carts table
-- ============================================================

ALTER TABLE carts
    ADD COLUMN promo_code     VARCHAR(50)     NULL,
    ADD COLUMN promo_discount DECIMAL(10,2)   NULL;

-- ============================================================
-- VegGo Fresh Platform — Flyway Migration V42
-- Customer Module: Extend orders table with many new fields
-- ============================================================

ALTER TABLE orders
    -- Human-readable order number, auto-generated at checkout
    ADD COLUMN order_number              VARCHAR(20)     NULL UNIQUE,

    -- Fee breakdown (set at checkout)
    ADD COLUMN delivery_fee              DECIMAL(10,2)   NULL,
    ADD COLUMN estimated_tax             DECIMAL(10,2)   NULL,

    -- Payment reference (UUID reference only — PaymentMethod lives in payment module)
    ADD COLUMN payment_method_id         VARCHAR(36)     NULL,

    -- Scheduled delivery preference
    ADD COLUMN scheduled_date            DATE            NULL,
    ADD COLUMN delivery_time_slot        VARCHAR(30)     NULL,

    -- Delivery agent info (populated by Delivery module via CustomerOrderService)
    ADD COLUMN delivery_agent_name       VARCHAR(100)    NULL,
    ADD COLUMN delivery_agent_phone      VARCHAR(20)     NULL,
    ADD COLUMN delivery_agent_photo_url  TEXT            NULL,
    ADD COLUMN estimated_delivery_window VARCHAR(50)     NULL,

    -- Status timestamps for progress timeline
    ADD COLUMN confirmed_at              TIMESTAMP(6)    NULL,
    ADD COLUMN preparing_at              TIMESTAMP(6)    NULL,
    ADD COLUMN out_for_delivery_at       TIMESTAMP(6)    NULL,
    ADD COLUMN delivered_at              TIMESTAMP(6)    NULL,
    ADD COLUMN cancelled_at              TIMESTAMP(6)    NULL,

    -- Proof of delivery (populated by Delivery module)
    ADD COLUMN delivery_photo_url        TEXT            NULL,
    ADD COLUMN delivery_location_note    VARCHAR(100)    NULL,

    -- Promo code applied at checkout
    ADD COLUMN promo_code                VARCHAR(50)     NULL,
    ADD COLUMN promo_discount            DECIMAL(10,2)   NULL;

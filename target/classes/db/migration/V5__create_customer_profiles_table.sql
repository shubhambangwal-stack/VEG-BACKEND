-- ============================================================
-- VegGo Fresh Platform — Flyway Migration V40
-- Customer Module: Customer Profiles Table
-- ============================================================

CREATE TABLE customer_profiles (
    id UUID NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    deleted_at TIMESTAMP(6) NULL,
    version BIGINT NOT NULL DEFAULT 0,
    user_id UUID NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_customer_profiles_user UNIQUE (user_id)
);

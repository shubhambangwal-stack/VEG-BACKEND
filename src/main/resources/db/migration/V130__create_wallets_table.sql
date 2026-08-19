-- ============================================================
-- VegGo Fresh Platform — Flyway Migration V130
-- Payment Module: Unified Wallet System
-- ============================================================

CREATE TABLE wallets (
    id                  VARCHAR(36)     NOT NULL PRIMARY KEY,
    created_at          TIMESTAMP       NOT NULL,
    updated_at          TIMESTAMP       NOT NULL,
    deleted_at          TIMESTAMP       NULL,
    version             BIGINT          NOT NULL DEFAULT 0,

    user_id             VARCHAR(36)     NOT NULL UNIQUE,
    role                VARCHAR(20)     NOT NULL,

    available_balance   DECIMAL(12,2)   NOT NULL DEFAULT 0.00,
    reserved_balance    DECIMAL(12,2)   NOT NULL DEFAULT 0.00
);

CREATE INDEX idx_wallets_user_id ON wallets (user_id);
CREATE INDEX idx_wallets_role    ON wallets (role);

CREATE TABLE wallet_transactions (
    id                  VARCHAR(36)     NOT NULL PRIMARY KEY,
    created_at          TIMESTAMP       NOT NULL,
    updated_at          TIMESTAMP       NOT NULL,
    deleted_at          TIMESTAMP       NULL,
    version             BIGINT          NOT NULL DEFAULT 0,

    wallet_id           VARCHAR(36)     NOT NULL,
    order_id            VARCHAR(36)     NULL,
    razorpay_payment_id VARCHAR(100)    NULL,
    type                VARCHAR(30)     NOT NULL,
    amount              DECIMAL(12,2)   NOT NULL,
    description         VARCHAR(255)    NOT NULL,

    CONSTRAINT fk_wallet_txn_wallet FOREIGN KEY (wallet_id) REFERENCES wallets (id)
);

CREATE INDEX idx_wallet_txn_wallet_id ON wallet_transactions (wallet_id);
CREATE INDEX idx_wallet_txn_order_id  ON wallet_transactions (order_id);
CREATE INDEX idx_wallet_txn_type      ON wallet_transactions (type);

-- ═══════════════════════════════════════════════════════════════════════
-- Wallet ledger. Rename this file to the next available V-number in
-- Payment's V130-V149 range before applying.
-- ═══════════════════════════════════════════════════════════════════════

CREATE TABLE wallets (
    id          UUID PRIMARY KEY,
    user_id     UUID NOT NULL UNIQUE,
    balance     NUMERIC(12,2) NOT NULL DEFAULT 0.00,
    created_at  TIMESTAMP NOT NULL,
    updated_at  TIMESTAMP,
    deleted_at  TIMESTAMP
);

CREATE TABLE wallet_transactions (
    id             UUID PRIMARY KEY,
    user_id        UUID NOT NULL,
    type           VARCHAR(20) NOT NULL,
    reason         VARCHAR(40) NOT NULL,
    amount         NUMERIC(12,2) NOT NULL,
    balance_after  NUMERIC(12,2) NOT NULL,
    reference_id   UUID,
    description    VARCHAR(255),
    created_at     TIMESTAMP NOT NULL,
    updated_at     TIMESTAMP,
    deleted_at     TIMESTAMP
);

CREATE INDEX idx_wallet_transactions_user_id_created_at
    ON wallet_transactions(user_id, created_at DESC);

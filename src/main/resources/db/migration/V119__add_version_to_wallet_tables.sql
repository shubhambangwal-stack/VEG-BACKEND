-- ═══════════════════════════════════════════════════════════════════════
-- V115__wallet.sql created wallets and wallet_transactions but omitted
-- `version`, the optimistic-locking column BaseEntity declares via @Version
-- that every other BaseEntity-backed table already has. Both tables in that
-- file are affected -- Hibernate only reports one missing column at a time,
-- so this covers both rather than waiting for a second failure on the next
-- run. Added here rather than editing V115, since V115 is already applied
-- and Flyway will not re-run a changed file.
-- ═══════════════════════════════════════════════════════════════════════

ALTER TABLE wallets
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE wallet_transactions
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
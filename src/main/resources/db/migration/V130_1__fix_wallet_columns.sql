-- Drop foreign key constraint first
ALTER TABLE wallet_transactions DROP CONSTRAINT IF EXISTS fk_wallet_txn_wallet;

-- Fix the column types from VARCHAR(36) to UUID
ALTER TABLE wallets ALTER COLUMN id TYPE UUID USING id::uuid;
ALTER TABLE wallets ALTER COLUMN user_id TYPE UUID USING user_id::uuid;

ALTER TABLE wallet_transactions ALTER COLUMN id TYPE UUID USING id::uuid;
ALTER TABLE wallet_transactions ALTER COLUMN wallet_id TYPE UUID USING wallet_id::uuid;
ALTER TABLE wallet_transactions ALTER COLUMN order_id TYPE UUID USING order_id::uuid;

-- Re-add foreign key constraint
ALTER TABLE wallet_transactions ADD CONSTRAINT fk_wallet_txn_wallet FOREIGN KEY (wallet_id) REFERENCES wallets (id);

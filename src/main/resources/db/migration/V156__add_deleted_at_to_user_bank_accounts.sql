-- ═══════════════════════════════════════════════════════════════════════
-- Add missing deleted_at column to user_bank_accounts table for BaseEntity soft delete support
-- ═══════════════════════════════════════════════════════════════════════

ALTER TABLE user_bank_accounts ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;

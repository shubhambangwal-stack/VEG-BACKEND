-- ============================================================
-- V163 -- Wishlist: uniqueness must only apply to ACTIVE rows
--
-- BUG: V8 created   CONSTRAINT uk_wishlists_user_product UNIQUE (user_id, product_id)
-- but removing a wishlist item is a SOFT delete (deleted_at is set, the row stays).
-- The entity hides soft-deleted rows (@Where deleted_at IS NULL), so after
-- "add -> remove" the "already in wishlist?" check finds nothing and the app
-- INSERTs again -- and the old constraint, which still counts the hidden row,
-- rejects it:
--     ERROR: duplicate key value violates unique constraint "uk_wishlists_user_product"
-- The customer got an HTTP 500 and could never wishlist that product again.
--
-- FIX: enforce "one ACTIVE row per customer + product" with a partial unique
-- index (the same pattern V80 already uses for vendor_listings). Soft-deleted
-- rows no longer take part in the uniqueness check, so re-adding always works.
--
-- Safe to run on existing data: every currently-active (user, product) pair is
-- already unique (the old, stricter constraint guaranteed it), so the new index
-- can always be built. Existing soft-deleted rows are left untouched.
-- ============================================================

ALTER TABLE wishlists DROP CONSTRAINT IF EXISTS uk_wishlists_user_product;

CREATE UNIQUE INDEX IF NOT EXISTS uk_wishlists_user_product_active
    ON wishlists (user_id, product_id)
    WHERE deleted_at IS NULL;

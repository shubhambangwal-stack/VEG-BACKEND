-- ═══════════════════════════════════════════════════════════════════════
-- Adds optimistic-locking version column to vendor_listings.
-- The entity uses @Version but the original V80 migration didn't include
-- a backing column — Hibernate schema validation fails without this.
-- ═══════════════════════════════════════════════════════════════════════

ALTER TABLE vendor_listings
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
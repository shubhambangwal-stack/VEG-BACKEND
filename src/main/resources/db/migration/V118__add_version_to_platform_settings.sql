-- ═══════════════════════════════════════════════════════════════════════
-- V114__platform_settings.sql created the table but omitted `version`,
-- the optimistic-locking column that BaseEntity declares via @Version and
-- that every other BaseEntity-backed table already has. Hibernate schema
-- validation fails without it. Adding it here rather than editing V114,
-- since V114 is already applied and Flyway will not re-run a changed file.
-- ═══════════════════════════════════════════════════════════════════════

ALTER TABLE platform_settings
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
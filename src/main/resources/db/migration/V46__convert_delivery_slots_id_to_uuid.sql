-- ============================================================
-- VegGo Fresh Platform — Flyway Migration V46
-- Customer Module: Convert delivery_slots id column to UUID for Postgres validation
-- ============================================================

-- Drop index if any exists on id or primary key mapping
ALTER TABLE delivery_slots DROP CONSTRAINT IF EXISTS delivery_slots_pkey CASCADE;

-- Convert column type to UUID
ALTER TABLE delivery_slots 
    ALTER COLUMN id TYPE UUID USING id::uuid;

-- Re-add primary key constraint
ALTER TABLE delivery_slots ADD PRIMARY KEY (id);

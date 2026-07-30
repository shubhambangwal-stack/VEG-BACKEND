-- ============================================================
-- VegGo Fresh Platform — Flyway Migration V43
-- Customer Module: Create delivery_slots table
-- ============================================================

CREATE TABLE delivery_slots (
    id         UUID         NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    deleted_at TIMESTAMP(6) NULL,
    version    BIGINT       NOT NULL DEFAULT 0,

    slot_date   DATE         NOT NULL,
    start_time  VARCHAR(5)   NOT NULL,   -- e.g. "09:00"
    end_time    VARCHAR(5)   NOT NULL,   -- e.g. "11:00"
    label       VARCHAR(30)  NOT NULL,   -- e.g. "09:00 - 11:00"
    is_available BOOLEAN     NOT NULL DEFAULT TRUE,

    PRIMARY KEY (id)
);

CREATE INDEX idx_delivery_slots_date ON delivery_slots(slot_date);

-- Seed some default slots so the API returns real data immediately
-- Cast the string literals to UUID compatibility
INSERT INTO delivery_slots (id, slot_date, start_time, end_time, label, is_available)
VALUES
    ('d1000000-0000-0000-0000-000000000001'::uuid, CURRENT_DATE,             '09:00', '11:00', '09:00 - 11:00', TRUE),
    ('d1000000-0000-0000-0000-000000000002'::uuid, CURRENT_DATE,             '11:00', '13:00', '11:00 - 13:00', TRUE),
    ('d1000000-0000-0000-0000-000000000003'::uuid, CURRENT_DATE,             '14:00', '16:00', '14:00 - 16:00', TRUE),
    ('d1000000-0000-0000-0000-000000000004'::uuid, CURRENT_DATE,             '17:00', '19:00', '17:00 - 19:00', TRUE),
    ('d1000000-0000-0000-0000-000000000005'::uuid, CURRENT_DATE + INTERVAL '1' DAY, '09:00', '11:00', '09:00 - 11:00', TRUE),
    ('d1000000-0000-0000-0000-000000000006'::uuid, CURRENT_DATE + INTERVAL '1' DAY, '11:00', '13:00', '11:00 - 13:00', TRUE),
    ('d1000000-0000-0000-0000-000000000007'::uuid, CURRENT_DATE + INTERVAL '1' DAY, '14:00', '16:00', '14:00 - 16:00', TRUE),
    ('d1000000-0000-0000-0000-000000000008'::uuid, CURRENT_DATE + INTERVAL '1' DAY, '17:00', '19:00', '17:00 - 19:00', TRUE);

-- ═══════════════════════════════════════════════════════════════════════
-- Idempotency ledger for inbound Razorpay webhooks. Razorpay retries
-- webhook delivery on non-2xx responses and can legitimately deliver the
-- same event twice even on a 200 -- every handler must be safe to run
-- twice, and the real guard for that is "have we already recorded this
-- razorpay_event_id", not clever handler-side dedup logic.
-- ═══════════════════════════════════════════════════════════════════════

CREATE TABLE payment_webhook_events (
    id                 UUID PRIMARY KEY,
    razorpay_event_id  VARCHAR(64) NOT NULL,
    event_type         VARCHAR(60) NOT NULL,
    payload            TEXT NOT NULL,
    processed_at       TIMESTAMP,
    created_at         TIMESTAMP NOT NULL,
    updated_at         TIMESTAMP,
    deleted_at         TIMESTAMP,
    version            BIGINT NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX idx_payment_webhook_events_event_id ON payment_webhook_events(razorpay_event_id);

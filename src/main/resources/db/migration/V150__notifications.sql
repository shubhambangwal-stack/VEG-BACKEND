-- ============================================================
-- VegGo Fresh — Notification Module (V150-V169 range)
-- V150: notifications table
--
-- Every notification is ALWAYS persisted first, before any
-- delivery attempt, so nothing is lost if the recipient is
-- offline. Real-time delivery rides on top of this row via
-- STOMP (see com.veggofresh.notification.config.WebSocketConfig)
-- and the REST fallback serves the same data to hydrate apps
-- on launch.
--
-- recipient_id   → the recipient's auth User UUID (loose ref,
--                  cross-module, no FK per convention).
-- recipient_role → CUSTOMER | VENDOR | DELIVERY | ADMIN
-- type           → NotificationType enum name, e.g. ORDER_PLACED.
-- data           → JSON text (order ids, shop ids, amounts …)
--                  opaque to the engine; forwarded as-is to clients.
-- ============================================================

CREATE TABLE notifications (
    id             UUID NOT NULL,
    created_at     TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at     TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    deleted_at     TIMESTAMP(6) NULL,
    version        BIGINT NOT NULL DEFAULT 0,
    recipient_id   UUID NOT NULL,
    recipient_role VARCHAR(20) NOT NULL,
    type           VARCHAR(50) NOT NULL,
    title          VARCHAR(255) NOT NULL,
    body           TEXT,
    data           TEXT,
    is_read        BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id)
);

-- Newest-first inbox per recipient + unread badge count lookups.
CREATE INDEX idx_notifications_recipient_created
    ON notifications(recipient_id, created_at DESC);

-- Unread-count query specifically (created_at included so the
-- combined index above is also a covering index for the common
-- inbox page (unread first) queries).
CREATE INDEX idx_notifications_recipient_read
    ON notifications(recipient_id, is_read);
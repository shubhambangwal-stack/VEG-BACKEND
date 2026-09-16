-- ============================================================
-- VegGo Fresh — User Device Tokens for FCM Push Notifications
-- V158: user_device_tokens table
-- ============================================================

CREATE TABLE user_device_tokens (
    id           UUID NOT NULL,
    created_at   TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at   TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    deleted_at   TIMESTAMP(6) NULL,
    version      BIGINT NOT NULL DEFAULT 0,
    user_id      UUID NOT NULL,
    fcm_token    VARCHAR(512) NOT NULL,
    platform     VARCHAR(20) NOT NULL DEFAULT 'ANDROID',
    last_used_at TIMESTAMP(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_user_device_tokens_fcm_token UNIQUE (fcm_token)
);

CREATE INDEX idx_user_device_tokens_user_id
    ON user_device_tokens(user_id);

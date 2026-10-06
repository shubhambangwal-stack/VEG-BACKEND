-- ============================================================
-- VegGo Fresh -- Chatbot module (V162)
-- Customer rating of a support-chat conversation.
--
-- One row per (customer, conversation). A customer may also SKIP the
-- rating; that is recorded too (skipped = TRUE, rating_value NULL) so the
-- admin can see how many chats ended without feedback.
--
-- The chat transcript itself is NOT stored -- the chatbot is stateless and
-- every answer is read live from the orders/cart/catalog tables.
-- tags and orders_discussed are client-reported analytics only.
-- ============================================================

CREATE TABLE chat_conversation_ratings (
    id UUID NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    deleted_at TIMESTAMP(6) NULL,
    version BIGINT NOT NULL DEFAULT 0,
    user_id UUID NOT NULL,
    conversation_id VARCHAR(40) NOT NULL,
    rating_value INT NULL,
    skipped BOOLEAN NOT NULL DEFAULT FALSE,
    tags VARCHAR(300),
    end_reason VARCHAR(30),
    message_count INT,
    orders_discussed VARCHAR(200),
    PRIMARY KEY (id),
    CONSTRAINT uk_chat_rating_user_conversation UNIQUE (user_id, conversation_id),
    CONSTRAINT ck_chat_rating_value CHECK (rating_value IS NULL OR rating_value BETWEEN 1 AND 5),
    CONSTRAINT ck_chat_rating_skipped CHECK (
        (skipped = TRUE AND rating_value IS NULL) OR (skipped = FALSE AND rating_value IS NOT NULL)
    ),
    CONSTRAINT fk_chat_rating_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX idx_chat_rating_created ON chat_conversation_ratings(created_at);
CREATE INDEX idx_chat_rating_value ON chat_conversation_ratings(rating_value);

-- ============================================================
-- VegGo Fresh — Delivery Module (V90-V109 range)
-- V102: Delivery Partner Ratings Table
--
-- Distinct from Customer module's own order/shop Rating entity. One rating
-- per completed assignment. See DeliveryRatingService for the cross-module
-- write contract -- not wired to anything yet, same situation as
-- DeliveryDispatchService. See NOTES.md.
-- ============================================================

CREATE TABLE delivery_partner_ratings (
    id UUID NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    deleted_at TIMESTAMP(6) NULL,
    version BIGINT NOT NULL DEFAULT 0,
    assignment_id UUID NOT NULL,
    delivery_partner_user_id UUID NOT NULL,
    customer_user_id UUID NOT NULL,
    rating_value INT NOT NULL,
    comment VARCHAR(1000),
    PRIMARY KEY (id),
    CONSTRAINT uk_delivery_partner_ratings_assignment UNIQUE (assignment_id),
    CONSTRAINT fk_delivery_partner_ratings_assignment FOREIGN KEY (assignment_id) REFERENCES delivery_assignments(id),
    CONSTRAINT fk_delivery_partner_ratings_partner FOREIGN KEY (delivery_partner_user_id) REFERENCES users(id),
    CONSTRAINT fk_delivery_partner_ratings_customer FOREIGN KEY (customer_user_id) REFERENCES users(id)
);

CREATE INDEX idx_delivery_partner_ratings_partner ON delivery_partner_ratings(delivery_partner_user_id);

-- Vendor accept/reject broadcast redesign -- Order.acceptedShopId + Order.rejectedShopIds.
-- Numbered V122 to continue this project's actual live migration sequence (confirmed
-- V119 was the latest applied before this session's V120/V121) rather than the
-- "next available in the 40s" the original patch notes assumed -- that guidance
-- predated this project's numbering having grown past V100.
ALTER TABLE orders ADD COLUMN accepted_shop_id UUID;

CREATE TABLE order_rejected_shops (
    order_id UUID NOT NULL REFERENCES orders(id),
    shop_id  UUID NOT NULL
);

CREATE INDEX idx_order_rejected_shops_order_id ON order_rejected_shops(order_id);

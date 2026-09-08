-- Adds the pack-size snapshot column on OrderItem introduced this round.
-- Nullable: existing historical orders placed before this column existed
-- will simply show no unit (there's no way to retroactively know what it
-- was at the time of that purchase, and that's fine -- new orders from this
-- point on always populate it, snapshotted at checkout same as price is).
ALTER TABLE order_items
    ADD COLUMN unit VARCHAR(100);
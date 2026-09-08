package com.veggofresh.customer.entity;

/**
 * NEW THIS ROUND: READY_FOR_PICKUP, inserted between PREPARING and OUT_FOR_DELIVERY.
 * Represents "vendor has marked this ready, delivery broadcast is underway, nobody has
 * picked it up yet" -- previously there was no status representing this at all; the
 * jump was PREPARING straight to OUT_FOR_DELIVERY.
 *
 * CONFIRMED -> READY_FOR_PICKUP is allowed DIRECTLY (skipping PREPARING) because nothing
 * in the codebase currently has any action that sets PREPARING -- there's no vendor
 * "start preparing" trigger anywhere. PREPARING -> READY_FOR_PICKUP stays valid too, in
 * case that gets built later; it's just not reachable from any real action today.
 *
 * CANCELLED is now reachable from PREPARING and READY_FOR_PICKUP too, not just
 * PLACED/CONFIRMED -- needed for CustomerOrderService.cancelOrderSystemInitiated(...)
 * (system-initiated cancellation from a Delivery re-broadcast limit hit), which can
 * legitimately fire while an order is sitting in READY_FOR_PICKUP waiting for a courier.
 * Note: cancelOrderSystemInitiated() and the customer-facing cancelOrder() both set
 * status directly rather than routing through isValidTransition -- this enum change is
 * about correctness/consistency for anything that DOES check it, not a hard requirement
 * for either of those two paths to keep working.
 */
public enum OrderStatus {
    PLACED,
    CONFIRMED,
    PREPARING,
    READY_FOR_PICKUP,
    OUT_FOR_DELIVERY,
    DELIVERED,
    CANCELLED;

    public boolean isValidTransition(OrderStatus nextStatus) {
        switch (this) {
            case PLACED:
                return nextStatus == CONFIRMED || nextStatus == CANCELLED;
            case CONFIRMED:
                return nextStatus == PREPARING || nextStatus == READY_FOR_PICKUP || nextStatus == CANCELLED;
            case PREPARING:
                return nextStatus == READY_FOR_PICKUP || nextStatus == CANCELLED;
            case READY_FOR_PICKUP:
                return nextStatus == OUT_FOR_DELIVERY || nextStatus == CANCELLED;
            case OUT_FOR_DELIVERY:
                return nextStatus == DELIVERED;
            case DELIVERED:
            case CANCELLED:
            default:
                return false;
        }
    }
}

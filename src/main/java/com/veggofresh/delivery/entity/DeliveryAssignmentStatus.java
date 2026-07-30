package com.veggofresh.delivery.entity;

public enum DeliveryAssignmentStatus {
    PENDING,
    ACCEPTED,
    ARRIVED_AT_STORE,
    PICKED_UP,
    ARRIVED_AT_DROP,
    DELIVERED,
    REJECTED,
    EXPIRED,
    CANCELLED;

    public boolean isValidTransition(DeliveryAssignmentStatus next) {
        return switch (this) {
            case PENDING -> next == ACCEPTED || next == REJECTED || next == EXPIRED || next == CANCELLED;
            case ACCEPTED -> next == ARRIVED_AT_STORE || next == CANCELLED;
            case ARRIVED_AT_STORE -> next == PICKED_UP || next == CANCELLED;
            case PICKED_UP -> next == ARRIVED_AT_DROP || next == CANCELLED;
            case ARRIVED_AT_DROP -> next == DELIVERED;
            default -> false;
        };
    }
}

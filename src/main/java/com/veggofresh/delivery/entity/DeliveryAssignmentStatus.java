package com.veggofresh.delivery.entity;

public enum DeliveryAssignmentStatus {
    PENDING,
    ACCEPTED,
    REJECTED,
    EXPIRED,
    PICKED_UP,
    DELIVERED,
    CANCELLED;

    public boolean isValidTransition(DeliveryAssignmentStatus next) {
        return switch (this) {
            case PENDING -> next == ACCEPTED || next == REJECTED || next == EXPIRED || next == CANCELLED;
            case ACCEPTED -> next == PICKED_UP || next == CANCELLED;
            case PICKED_UP -> next == DELIVERED;
            default -> false;
        };
    }
}

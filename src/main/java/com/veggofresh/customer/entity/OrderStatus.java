package com.veggofresh.customer.entity;

public enum OrderStatus {
    PLACED,
    CONFIRMED,
    PREPARING,
    OUT_FOR_DELIVERY,
    DELIVERED,
    CANCELLED;

    public boolean isValidTransition(OrderStatus nextStatus) {
        switch (this) {
            case PLACED:
                return nextStatus == CONFIRMED || nextStatus == CANCELLED;
            case CONFIRMED:
                return nextStatus == PREPARING || nextStatus == CANCELLED;
            case PREPARING:
                return nextStatus == OUT_FOR_DELIVERY;
            case OUT_FOR_DELIVERY:
                return nextStatus == DELIVERED;
            case DELIVERED:
            case CANCELLED:
            default:
                return false;
        }
    }
}

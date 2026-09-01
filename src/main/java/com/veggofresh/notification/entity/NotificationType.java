package com.veggofresh.notification.entity;

/**
 * Machine-readable type of a notification. Drives client-side rendering
 * decisions (which screen to refresh, which icon to show) and doubles as a
 * stable audit label for the row in {@code notifications}.
 *
 * <p>Ordered to match the event coverage implemented for this module:
 * order lifecycle → delivery lifecycle → stock/payment/admin events.
 */
public enum NotificationType {
    // ── Order lifecycle ──────────────────────────────────────
    ORDER_PLACED,
    NEW_ORDER_REQUEST,          // vendor-side broadcast-inbox ping (order awaiting acceptance)
    ORDER_ACCEPTED,             // vendor-side: this shop won the accept race
    ORDER_CONFIRMED,            // customer-side
    ORDER_PACKED,               // customer-side: vendor started/prepared
    ORDER_READY_FOR_PICKUP,     // customer-side: packed & handed off to delivery network
    ORDER_OUT_FOR_DELIVERY,
    ORDER_DELIVERED,
    ORDER_CANCELLED,

    // ── Delivery lifecycle ────────────────────────────────────
    DELIVERY_ASSIGNED,          // partner accepted / was assigned an order
    DELIVERY_PICKUP_OTP_GENERATED,
    DELIVERY_PICKUP_OTP_VERIFIED,
    DELIVERY_DROP_OTP_GENERATED,
    DELIVERY_DROP_OTP_VERIFIED,

    // ── Stock / money / feedback ─────────────────────────────
    LOW_STOCK_ALERT,
    PAYMENT_SUCCESS,
    PAYMENT_FAILED,
    REVIEW_RECEIVED,

    // ── Admin ────────────────────────────────────────────────
    ADMIN_ANNOUNCEMENT
}
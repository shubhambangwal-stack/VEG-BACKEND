package com.veggofresh.customer.service;

import com.veggofresh.customer.dto.response.OrderResponseDto;

import java.util.List;
import java.util.UUID;

/**
 * Cross-module interface owned by the Customer module.
 * Called by the Vendor module and the Delivery module.
 * Never import Customer @Entity classes outside this module.
 */
public interface CustomerOrderService {

    // ── Vendor-facing methods ──────────────────────────────
    void acceptOrder(UUID orderId);
    void rejectOrder(UUID orderId);
    void updateOrderStatus(UUID orderId, String status);
    List<OrderResponseDto> getOrdersByShopId(UUID shopId);

    // ── Delivery-facing methods ──────────────────────────

    void assignDeliveryAgent(UUID orderId, String agentName, String agentPhone,
                             String agentPhotoUrl, String estimatedWindow);

    void markDelivered(UUID orderId, String deliveryPhotoUrl, String locationNote);

    /**
     * NEW THIS ROUND -- system-initiated cancellation, called by Delivery (re-broadcast
     * limit hit) or, once built, Vendor's own broadcast leg. Unlike the customer-facing
     * cancelOrder(userId, orderId), this is NOT restricted to PLACED/CONFIRMED status and
     * does NOT check order ownership by a specific customer -- the caller is a system
     * process reacting to a broadcast failure, not a user clicking cancel. Still performs
     * the same wallet refund as customer-initiated cancellation. Safe to call on an
     * already-terminal order (DELIVERED/CANCELLED) -- becomes a no-op rather than
     * throwing, since the caller (a background sweep) shouldn't need to pre-check state.
     */
    void cancelOrderSystemInitiated(UUID orderId, String reason);

    /**
     * LEGACY — do not switch to this. Weak hashCode-derived OTP, no expiry,
     * no attempt limit. Kept only because it's a named interface method
     * something might still call; Delivery's own real OTP system (random,
     * time-limited, attempt-capped) is the real implementation. See
     * PROJECT_STATE "Newly discovered Customer methods" section.
     */
    String getDeliveryOtp(UUID orderId);
}

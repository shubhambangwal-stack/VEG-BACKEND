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

    // ── Delivery-facing methods (GAP 18) ──────────────────

    /**
     * Called by Delivery module when a delivery agent is assigned to an order.
     */
    void assignDeliveryAgent(UUID orderId, String agentName, String agentPhone,
                             String agentPhotoUrl, String estimatedWindow);

    /**
     * Called by Delivery module upon delivery completion.
     */
    void markDelivered(UUID orderId, String deliveryPhotoUrl, String locationNote);

    /**
     * Returns the OTP for order handover. Called by Delivery module.
     */
    String getDeliveryOtp(UUID orderId);
}

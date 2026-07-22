package com.veggofresh.customer.service;

import java.util.UUID;

/**
 * Stub interface for Customer Order Service.
 * Owned by the Customer module, but stubbed here to unblock Vendor module development.
 */
public interface CustomerOrderService {
    void acceptOrder(UUID orderId);
    void rejectOrder(UUID orderId);
    void updateOrderStatus(UUID orderId, String status);
}

package com.veggofresh.delivery.service;

import java.util.UUID;

/**
 * Public cross-module interface for triggering delivery dispatch.
 * Owned by the Delivery module — this is the ONLY way other modules request a delivery
 * assignment. They must never insert DeliveryAssignment rows directly.
 *
 * Intended caller: Customer module (or Vendor module, on order acceptance) should invoke
 * dispatchOrder(...) once an order transitions to CONFIRMED, passing the shop's pickup
 * coordinates and the customer's delivery-address coordinates.
 *
 * NOTE for integration: this currently isn't wired to any real trigger — Customer's
 * OrderServiceImpl.updateOrderStatus() does not yet call this. That wiring belongs in
 * whichever module owns the CONFIRMED transition. See NOTES.md.
 */
public interface DeliveryDispatchService {
    void dispatchOrder(UUID orderId, double pickupLat, double pickupLng, double dropLat, double dropLng);
}

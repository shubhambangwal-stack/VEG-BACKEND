package com.veggofresh.delivery.service;

import java.util.UUID;

/**
 * Public cross-module interface for triggering delivery dispatch.
 * Owned by the Delivery module — this is the ONLY way other modules request a delivery
 * assignment. They must never insert DeliveryAssignment rows directly.
 *
 * Intended caller: Customer module (or Vendor module, on order acceptance) should invoke
 * dispatchOrder(...) once an order transitions to CONFIRMED.
 *
 * PHASE B CHANGE: now also takes customerUserId, shopOwnerUserId, shopName, shopAddress.
 * customerUserId/shopOwnerUserId let Delivery resolve live phone numbers via
 * UserLookupService. shopName/shopAddress are a SNAPSHOT taken at dispatch time — Vendor
 * module has no ShopLookupService yet to resolve this live. See NOTES.md. Whoever wires
 * the real trigger should pass Shop.ownerUserId, Shop.name, Shop.address, and the
 * customer's userId (Order.userId) directly — no new lookups required on the caller's side.
 *
 * NOTE for integration: this currently isn't wired to any real trigger — Customer's
 * OrderServiceImpl.updateOrderStatus() does not yet call this.
 */
public interface DeliveryDispatchService {
    void dispatchOrder(UUID orderId, UUID customerUserId, UUID shopOwnerUserId, String shopName, String shopAddress,
                        double pickupLat, double pickupLng, double dropLat, double dropLng);
}

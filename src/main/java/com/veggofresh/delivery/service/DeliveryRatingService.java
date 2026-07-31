package com.veggofresh.delivery.service;

import java.util.UUID;

/**
 * Public cross-module interface for rating a delivery partner. Owned by Delivery module
 * -- this is the ONLY way other modules record a partner rating. Distinct from Customer
 * module's own order/shop Rating entity, confirmed as a separate concept.
 *
 * NOTE for integration: not wired to anything yet, same situation as
 * DeliveryDispatchService. Whoever owns Customer's rateOrder flow (currently only rates
 * the order/shop, see OrderServiceImpl.rateOrder) should call this alongside it once an
 * order reaches DELIVERED, passing the order's own id (this module resolves the
 * assignment and partner internally).
 */
public interface DeliveryRatingService {
    void rateDeliveryPartner(UUID orderId, UUID customerUserId, int ratingValue, String comment);
}

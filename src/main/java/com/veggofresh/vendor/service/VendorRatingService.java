package com.veggofresh.vendor.service;

import java.util.UUID;

/**
 * Public cross-module interface for rating a shop. Owned by Vendor module -- this is
 * the ONLY way other modules record a shop rating. Distinct from Customer module's own
 * order/product Rating entity and from Delivery's DeliveryRatingService.
 *
 * shopId must be supplied by the caller (Customer module) -- Vendor can't derive which
 * shop within a multi-vendor order is being rated without it. Customer's checkout/order
 * data already ties line items to shops via product ownership, so the caller should
 * call this once per distinct shop present in the order.
 *
 * NOT WIRED to anything yet -- same situation as DeliveryRatingService. Customer's
 * OrderServiceImpl.rateOrder currently only rates the order/product; whoever owns that
 * flow should call this alongside it for each shop in the order.
 */
public interface VendorRatingService {
    void rateShop(UUID orderId, UUID shopId, UUID customerUserId, int ratingValue, String comment);
}

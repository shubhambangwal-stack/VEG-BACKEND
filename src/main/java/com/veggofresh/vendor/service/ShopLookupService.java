package com.veggofresh.vendor.service;

import java.util.Optional;
import java.util.UUID;

/**
 * Public cross-module interface for resolving the owning {@code User} of a
 * shop. Owned by the Vendor module — other modules (Customer event wiring,
 * Delivery OTP flows, Notification module) use this instead of importing the
 * {@code Shop} entity directly.
 *
 * <p>Primary need: the notification engine pushes vendor-targeted messages to
 * the shop OWNER's user id ({@code Shop.ownerUserId}), but only a shop id is
 * normally in scope at the event call site.
 */
public interface ShopLookupService {

    /**
     * @param shopId the vendor shop id
     * @return the shop owner's auth user id, or {@link Optional#empty()} if the
     *         shop id is unknown or soft-deleted
     */
    Optional<UUID> findOwnerUserIdByShopId(UUID shopId);
}
package com.veggofresh.vendor.service;

import com.veggofresh.vendor.dto.ShopSummaryDto;

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

    /**
     * NEW -- resolves the shop's own public identity (display name, business
     * contact number, address) for cross-module display once a viewer is
     * entitled to see it: Customer's order detail once a shop has accepted the
     * order, and Delivery's assignment detail once a partner has accepted the
     * assignment. Deliberately returns the shop's {@code businessPhone}, not
     * the owner's personal phone number (those can differ) -- callers that
     * previously resolved the owner's personal phone via UserLookupService as a
     * stand-in for "the shop's number" should migrate to this instead.
     *
     * @param shopId the vendor shop id
     * @return the shop summary, or {@link Optional#empty()} if the shop id is
     *         unknown or soft-deleted
     */
    Optional<ShopSummaryDto> findShopSummaryById(UUID shopId);

    /**
     * NEW -- reverse of findShopSummaryById, for callers that only have the
     * shop owner's user id in scope (Delivery's DeliveryAssignment denormalizes
     * shopOwnerUserId, not shopId -- see that entity's own javadoc). Used to
     * resolve the shop's real business phone instead of falling back to the
     * owner's personal phone number.
     *
     * @param ownerUserId the shop owner's auth user id
     * @return the shop summary, or {@link Optional#empty()} if no shop is
     *         currently owned by this user
     */
    Optional<ShopSummaryDto> findShopSummaryByOwnerUserId(UUID ownerUserId);
}
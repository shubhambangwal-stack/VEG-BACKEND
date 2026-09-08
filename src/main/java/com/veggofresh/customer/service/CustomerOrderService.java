package com.veggofresh.customer.service;

import com.veggofresh.customer.dto.response.OrderResponseDto;
import com.veggofresh.customer.dto.response.OrderSettlementDto;

import java.util.List;
import java.util.UUID;

/**
 * Cross-module interface owned by the Customer module.
 * Called by the Vendor module and the Delivery module.
 * Never import Customer @Entity classes outside this module.
 */
public interface CustomerOrderService {

    // ── Vendor-facing methods ──────────────────────────────
    // BREAKING CHANGE THIS ROUND: acceptOrder/rejectOrder now require a shopId.
    // Previously took none at all -- that was the root cause of a real bug: an order
    // accepted by one vendor stayed visible and actionable by every other original
    // candidate, since nothing recorded who actually won. getOrdersByShopId is REMOVED
    // entirely, split into two methods with genuinely different meanings -- see each
    // one's own javadoc. Full detail in NOTES_CUSTOMER.md.

    /**
     * Real atomic accept -- one conditional UPDATE, not a read-then-write. Throws
     * ORDER_ALREADY_ACCEPTED (409) if another shop's accept already won the race for
     * this order; ORDER_NOT_ACCEPTABLE (400) if the order is no longer in a state
     * where accepting makes sense at all (already delivered/cancelled).
     */
    void acceptOrder(UUID orderId, UUID shopId);

    /**
     * Narrows the candidate pool -- does NOT cancel the order by itself. Adds shopId
     * to the order's rejected set; if that empties the remaining candidate pool
     * (every original candidate has now rejected), cancels for real via
     * cancelOrderSystemInitiated. Throws ORDER_NOT_A_CANDIDATE if shopId was never in
     * this order's candidateVendorIds; ORDER_NOT_REJECTABLE if the order has already
     * moved past PLACED (someone else already accepted, or it's already terminal).
     */
    void rejectOrder(UUID orderId, UUID shopId);

    void updateOrderStatus(UUID orderId, String status);

    /**
     * NEW -- the broadcast inbox. Every order still awaiting a decision that this shop
     * is still eligible to act on (candidate, not yet accepted by anyone, hasn't
     * rejected it themselves). Disappears from this list the instant ANY shop accepts,
     * or this shop rejects, or the vendor-accept-timeout sweep cancels it.
     */
    List<OrderResponseDto> getOrderRequestsForShop(UUID shopId);

    /**
     * NEW -- a shop's real order history. Only orders THIS shop actually won the
     * accept race for -- never orders they were merely a candidate for. This is what
     * GET /api/vendor/orders now means; markReadyForPickup and every other
     * post-accept action are scoped against this, not candidacy.
     */
    List<OrderResponseDto> getAcceptedOrdersForShop(UUID shopId);

    // ── Delivery-facing methods ──────────────────────────

    void assignDeliveryAgent(UUID orderId, String agentName, String agentPhone,
                             String agentPhotoUrl, String estimatedWindow);

    void markDelivered(UUID orderId, String deliveryPhotoUrl, String locationNote);

    /**
     * NEW — called by Delivery's issueDropOtp() every time a drop OTP is generated:
     * initial issuance (right when the delivery partner marks "picked up") or a later
     * regenerateDropOtp() call. Pushes the REAL drop OTP (Delivery's own DeliveryOtp
     * system: random, time-limited, attempt-capped) into this order so it becomes
     * visible on the customer's track response (and the dedicated drop-OTP GET
     * endpoint) from this moment on -- not before. This is the real replacement for
     * the legacy {@link #getDeliveryOtp(UUID)} stand-in below; that method is no
     * longer called by anything and is kept only because removing an interface method
     * is out of scope here.
     */
    void setDropOtpAvailable(UUID orderId, String dropOtp);

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
     * SUPERSEDED THIS ROUND by {@link #setDropOtpAvailable(UUID, String)} — the real
     * drop OTP now reaches this module properly; this method has no callers left.
     */
    String getDeliveryOtp(UUID orderId);

    /**
     * Returns the minimal settlement fields of an Order needed by the Delivery
     * module to trigger wallet settlement after delivery completes.
     * Throws ORDER_NOT_FOUND if the order doesn't exist.
     * Module-boundary-safe: returns a DTO, never the Order @Entity.
     */
    OrderSettlementDto getOrderForSettlement(UUID orderId);
}

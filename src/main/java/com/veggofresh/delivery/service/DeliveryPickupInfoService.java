package com.veggofresh.delivery.service;

import com.veggofresh.delivery.dto.VendorDeliveryStatusDto;

import java.util.UUID;

/**
 * NEW LAST ROUND, EXTENDED THIS ROUND -- cross-module interface for Vendor's
 * order-detail view. getPickupOtpForVendor was built last round (Delivery); the second
 * method was added this round (Vendor) to also surface live delivery status + partner
 * contact info, broadening this interface rather than fragmenting into a second tiny
 * one for the same "vendor-facing delivery info" concept.
 */
public interface DeliveryPickupInfoService {

    /**
     * The current pickup OTP for this order, if any. Returns null if no delivery
     * partner has accepted yet (nothing to hand over), or if the pickup OTP has already
     * been verified (pickup already happened -- showing a stale OTP would be confusing).
     *
     * @param shopOwnerUserId caller's own user id, checked against the assignment's
     *                        recorded shopOwnerUserId -- a vendor can only ever see the
     *                        OTP for their own shop's orders, never another shop's.
     */
    String getPickupOtpForVendor(UUID orderId, UUID shopOwnerUserId);

    /**
     * NEW THIS ROUND -- current delivery status and, once accepted, the assigned
     * partner's name/phone. Ownership-checked the same way as getPickupOtpForVendor.
     * Looks at the MOST RECENT assignment round for this order (multiple rounds can
     * exist after re-broadcasts) -- always the current, relevant one.
     */
    VendorDeliveryStatusDto getDeliveryStatusForVendor(UUID orderId, UUID shopOwnerUserId);
}

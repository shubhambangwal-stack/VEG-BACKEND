package com.veggofresh.delivery.service;

import com.veggofresh.delivery.dto.response.DeliveryAssignmentResponseDto;
import com.veggofresh.delivery.dto.response.ProofOfDeliveryResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface DeliveryAssignmentService {
    /** radiusKm is a client preference for narrowing their own view, clamped server-side to never exceed Admin's configured delivery radius -- it cannot be used to see assignments they're not actually eligible for. */
    List<DeliveryAssignmentResponseDto> getNearbyAssignments(UUID deliveryPartnerUserId, double lat, double lng, double radiusKm);

    /**
     * NEW THIS ROUND: real atomic accept (DeliveryAssignmentRepository.atomicClaim) plus
     * a real radius check against the partner's own current location -- previously
     * neither existed (see NOTES_DELIVERY.md). Issues the pickup OTP and calls
     * CustomerOrderService.assignDeliveryAgent(...) on success -- replacing an incorrect
     * prior call to acceptOrder(...), which was semantically wrong here (that's the
     * vendor's action on the order, not the delivery partner's action on the assignment)
     * and would start throwing INVALID_ORDER_STATE_TRANSITION once the order is already
     * past PLACED by the time delivery gets involved, which is now always the case.
     */
    DeliveryAssignmentResponseDto acceptAssignment(UUID deliveryPartnerUserId, UUID orderId);

    DeliveryAssignmentResponseDto rejectAssignment(UUID deliveryPartnerUserId, UUID orderId);

    /**
     * NEW THIS ROUND: cancel-after-accept, previously didn't exist at all. Only valid
     * from ACCEPTED or ARRIVED_AT_STORE (i.e. before the goods are physically in the
     * partner's hands) -- once PICKED_UP, cancellation is out of scope for this round.
     * Triggers the same bounded re-broadcast loop as a round timing out with nobody
     * accepting (see rejectAssignment/expireStaleAssignments), excluding this partner.
     */
    DeliveryAssignmentResponseDto cancelAssignment(UUID deliveryPartnerUserId, UUID orderId, String reason);

    DeliveryAssignmentResponseDto markArrivedAtStore(UUID deliveryPartnerUserId, UUID orderId);

    /** NEW REQUIREMENT THIS ROUND: now requires the pickup OTP to be verified first -- see verifyPickupOtp(). Mirrors how completeDelivery() already requires the drop OTP verified first. */
    DeliveryAssignmentResponseDto markPickedUp(UUID deliveryPartnerUserId, UUID orderId);

    DeliveryAssignmentResponseDto markArrivedAtDrop(UUID deliveryPartnerUserId, UUID orderId);

    /** NEW THIS ROUND: verifies the vendor-issued pickup OTP. Callable while ARRIVED_AT_STORE, before markPickedUp(). */
    void verifyPickupOtp(UUID deliveryPartnerUserId, UUID orderId, String otp);

    /** Pre-existing drop-OTP verification, unchanged in behavior -- now 6 digits instead of 4, see NOTES_DELIVERY.md. */
    void verifyDeliveryOtp(UUID deliveryPartnerUserId, UUID orderId, String otp);

    /**
     * NEW -- lets the delivery partner request a fresh pickup OTP (e.g. the original
     * expired before they reached the store). Callable while ACCEPTED or
     * ARRIVED_AT_STORE. Overwrites the same DeliveryOtp row (resets code, expiry,
     * verified, attempts) -- Vendor's existing GET pickup-otp endpoint picks up the
     * new value on its next call automatically, since that endpoint is a live read.
     */
    void regeneratePickupOtp(UUID deliveryPartnerUserId, UUID orderId);

    /**
     * NEW -- same idea for the drop OTP. Callable while PICKED_UP or ARRIVED_AT_DROP.
     * Immediately re-pushes the new code to the customer's order via
     * CustomerOrderService.setDropOtpAvailable() -- both the customer's track response
     * and the dedicated GET drop-OTP endpoint reflect the new code right away.
     */
    void regenerateDropOtp(UUID deliveryPartnerUserId, UUID orderId);

    ProofOfDeliveryResponseDto submitProofOfDelivery(UUID deliveryPartnerUserId, UUID orderId, MultipartFile photo,
                                                       boolean deliveredToCustomerDirectly, boolean leftAtFrontDoor,
                                                       boolean packagingIntact, boolean addressVerifiedManually, String notes);

    DeliveryAssignmentResponseDto completeDelivery(UUID deliveryPartnerUserId, UUID orderId);

    /** Sweeps PENDING assignments past expiresAt -- unchanged trigger, but expiresAt duration and the reassign() it triggers now both read Admin's real configured settings instead of hardcoded constants. */
    void expireStaleAssignments();

    DeliveryAssignmentResponseDto getAssignmentDetail(UUID deliveryPartnerUserId, UUID orderId);

    Page<DeliveryAssignmentResponseDto> getMyOrders(UUID deliveryPartnerUserId, String status, Pageable pageable);

    /**
     * Creates the first PENDING assignment for a newly ready-for-pickup order. Called by
     * DeliveryDispatchService. NEW THIS ROUND: expiresAt duration now reads Admin's
     * configured deliveryAcceptTimeoutSeconds instead of a hardcoded constant.
     */
    void createAssignmentForOrder(UUID orderId, UUID customerUserId, UUID shopOwnerUserId, String shopName, String shopAddress,
                                   double pickupLat, double pickupLng, double dropLat, double dropLng);
}

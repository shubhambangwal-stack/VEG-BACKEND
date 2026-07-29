package com.veggofresh.delivery.service;

import com.veggofresh.delivery.dto.response.DeliveryAssignmentResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface DeliveryAssignmentService {
    List<DeliveryAssignmentResponseDto> getNearbyAssignments(UUID deliveryPartnerUserId, double lat, double lng, double radiusKm);
    DeliveryAssignmentResponseDto acceptAssignment(UUID deliveryPartnerUserId, UUID orderId);
    DeliveryAssignmentResponseDto rejectAssignment(UUID deliveryPartnerUserId, UUID orderId);
    DeliveryAssignmentResponseDto markArrivedAtStore(UUID deliveryPartnerUserId, UUID orderId);
    DeliveryAssignmentResponseDto markPickedUp(UUID deliveryPartnerUserId, UUID orderId);
    DeliveryAssignmentResponseDto markArrivedAtDrop(UUID deliveryPartnerUserId, UUID orderId);
    void verifyDeliveryOtp(UUID deliveryPartnerUserId, UUID orderId, String otp);
    DeliveryAssignmentResponseDto completeDelivery(UUID deliveryPartnerUserId, UUID orderId);
    void expireStaleAssignments();

    /** Full detail for one assignment: contact info (resolved live) + full status timeline. */
    DeliveryAssignmentResponseDto getAssignmentDetail(UUID deliveryPartnerUserId, UUID orderId);

    /** status: "active" (anything not DELIVERED/REJECTED/EXPIRED/CANCELLED) or "completed" (DELIVERED only). */
    Page<DeliveryAssignmentResponseDto> getMyOrders(UUID deliveryPartnerUserId, String status, Pageable pageable);

    /**
     * Creates the first PENDING assignment for a newly confirmed order, matched to the
     * nearest available online + KYC-approved partner. Called by DeliveryDispatchService.
     */
    void createAssignmentForOrder(UUID orderId, UUID customerUserId, UUID shopOwnerUserId, String shopName, String shopAddress,
                                   double pickupLat, double pickupLng, double dropLat, double dropLng);
}

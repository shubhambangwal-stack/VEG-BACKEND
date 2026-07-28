package com.veggofresh.delivery.service;

import com.veggofresh.delivery.dto.response.DeliveryAssignmentResponseDto;

import java.util.List;
import java.util.UUID;

public interface DeliveryAssignmentService {
    List<DeliveryAssignmentResponseDto> getNearbyAssignments(UUID deliveryPartnerUserId, double lat, double lng, double radiusKm);
    DeliveryAssignmentResponseDto acceptAssignment(UUID deliveryPartnerUserId, UUID orderId);
    DeliveryAssignmentResponseDto rejectAssignment(UUID deliveryPartnerUserId, UUID orderId);
    DeliveryAssignmentResponseDto markPickedUp(UUID deliveryPartnerUserId, UUID orderId);
    void verifyDeliveryOtp(UUID deliveryPartnerUserId, UUID orderId, String otp);
    DeliveryAssignmentResponseDto completeDelivery(UUID deliveryPartnerUserId, UUID orderId);
    void expireStaleAssignments();

    /**
     * Creates the first PENDING assignment for a newly confirmed order, matched to the
     * nearest available online + KYC-approved partner. Called by DeliveryDispatchService —
     * this is the entry point other modules use to trigger dispatch.
     */
    void createAssignmentForOrder(UUID orderId, double pickupLat, double pickupLng, double dropLat, double dropLng);
}

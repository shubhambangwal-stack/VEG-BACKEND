package com.veggofresh.delivery.dto.response;

import com.veggofresh.delivery.entity.DeliveryAssignmentStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class DeliveryAssignmentResponseDto {
    private UUID id;
    private UUID orderId;
    private DeliveryAssignmentStatus status;
    private double pickupLatitude;
    private double pickupLongitude;
    private double dropLatitude;
    private double dropLongitude;
    private Instant assignedAt;
    private Instant expiresAt;
}

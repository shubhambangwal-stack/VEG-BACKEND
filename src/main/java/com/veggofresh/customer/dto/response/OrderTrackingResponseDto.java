package com.veggofresh.customer.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderTrackingResponseDto {
    private UUID orderId;
    private String status;
    private double currentLatitude;
    private double currentLongitude;
    private String deliveryAgentName;
    private String deliveryAgentPhone;
    private Instant estimatedDeliveryTime;
}

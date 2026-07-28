package com.veggofresh.customer.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderTrackingResponseDto {
    private UUID orderId;
    private String orderNumber;
    private String status;
    private String deliveryAddress;
    private String estimatedDeliveryWindow;   // "Today, 2:30 PM – 4:00 PM"

    // Live delivery agent coordinates (simulated offset from delivery address)
    private double currentLatitude;
    private double currentLongitude;

    // Delivery agent info
    private String deliveryAgentName;
    private String deliveryAgentPhone;
    private String deliveryAgentPhotoUrl;

    // Status progress timeline
    private List<StatusTimelineDto> statusTimeline;

    // Order items in the tracking screen
    private List<OrderItemResponseDto> items;

    private BigDecimal total;

    // Post-delivery fields (visible after DELIVERED status)
    private String deliveryPhotoUrl;
    private String deliveryLocationNote;
    private Instant deliveredAt;
    private boolean hasBeenRated;
}

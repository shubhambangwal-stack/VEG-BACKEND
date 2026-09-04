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
    private String estimatedDeliveryWindow;
    private double currentLatitude;
    private double currentLongitude;
    private String deliveryAgentName;
    private String deliveryAgentPhone;
    private String deliveryAgentPhotoUrl;
    private List<StatusTimelineDto> statusTimeline;
    private List<OrderItemResponseDto> items;
    private BigDecimal total;
    private String deliveryPhotoUrl;
    private String deliveryLocationNote;
    private Instant deliveredAt;
    private boolean hasBeenRated;

    /**
     * Null until the drop OTP is generated -- pushed in by
     * CustomerOrderService.setDropOtpAvailable() the moment the delivery partner marks
     * "picked up" (or later, if regenerated). Also independently retrievable via the
     * dedicated {@code GET /api/customer/orders/{id}/drop-otp} endpoint, which reads
     * this same value.
     */
    private String dropOtp;
}

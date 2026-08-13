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
public class OrderResponseDto {
    private UUID id;
    private UUID userId;
    private String orderNumber;
    private String status;
    private BigDecimal totalAmount;
    private BigDecimal deliveryFee;
    private BigDecimal estimatedTax;
    private BigDecimal promoDiscount;
    private String promoCode;
    private String deliveryAddress;
    private double latitude;
    private double longitude;
    private String scheduledDate;
    private String deliveryTimeSlot;
    private String paymentMethod;
    private int itemCount;
    private List<String> itemThumbnails;
    private String estimatedDeliveryWindow;
    private boolean canTrack;
    private boolean canReorder;
    private boolean canCancel;
    private List<OrderItemResponseDto> items;
    private Instant createdAt;
    private Instant updatedAt;
}

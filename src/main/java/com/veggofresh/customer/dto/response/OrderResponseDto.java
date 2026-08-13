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
    private String orderNumber;        // e.g. "#VG-2940582"
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
    private String deliveryTimeSlot;   // "09:00 - 11:00"
    private String paymentMethod;      // display label: "Razorpay", "COD", etc.
    /** Payment lifecycle status: CREATED | CAPTURED | FAILED | null (legacy/COD) */
    private String paymentStatus;
    /** Razorpay order ID — null for COD / pre-payment-module orders */
    private String razorpayOrderId;
    private int itemCount;             // quick badge count
    private List<String> itemThumbnails; // first 2-3 product image URLs
    private String estimatedDeliveryWindow; // "Today, 2:30 PM – 4:00 PM"
    private boolean canTrack;          // status == OUT_FOR_DELIVERY
    private boolean canReorder;        // status == DELIVERED
    private boolean canCancel;         // status == PLACED or CONFIRMED
    private List<OrderItemResponseDto> items;
    private Instant createdAt;
    private Instant updatedAt;
}

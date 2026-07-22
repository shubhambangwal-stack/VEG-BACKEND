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
    private String status;
    private BigDecimal totalAmount;
    private String deliveryAddress;
    private double latitude;
    private double longitude;
    private List<OrderItemResponseDto> items;
    private Instant createdAt;
    private Instant updatedAt;
}

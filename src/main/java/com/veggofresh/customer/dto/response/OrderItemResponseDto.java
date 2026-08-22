package com.veggofresh.customer.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemResponseDto {
    private UUID id;
    private UUID productId;
    private String productName;
    private int quantity;
    private BigDecimal price;
    private String unit;
    private BigDecimal subTotal;
}

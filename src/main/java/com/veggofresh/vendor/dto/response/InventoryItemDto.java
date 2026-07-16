package com.veggofresh.vendor.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class InventoryItemDto {
    private UUID id;
    private UUID productId;
    private int stockQuantity;
    private int lowStockThreshold;
}

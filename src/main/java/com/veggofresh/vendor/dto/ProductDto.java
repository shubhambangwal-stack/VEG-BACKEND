package com.veggofresh.vendor.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductDto {
    private UUID id;
    private String name;
    private BigDecimal price;
    private String description;
    private UUID shopId;
    private String shopName;
    private String category;
    private String imageUrl;

    // GAP 8 fields
    private String unit;
    private boolean isBestSeller;
    private Integer discountPercent;
    private String badge;
    private List<String> whyItsGreat;
    private String storageTips;
}

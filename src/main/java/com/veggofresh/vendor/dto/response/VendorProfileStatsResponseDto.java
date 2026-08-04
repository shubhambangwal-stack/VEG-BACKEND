package com.veggofresh.vendor.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/** Figma "Vendor Profile Hub" stats row (Total Sales / Store Rating / Active Items). */
@Getter
@Builder
public class VendorProfileStatsResponseDto {
    private BigDecimal totalSales;
    private Double storeRating;
    private long ratingCount;
    private long activeItemsCount;
    private boolean isVerified;
}

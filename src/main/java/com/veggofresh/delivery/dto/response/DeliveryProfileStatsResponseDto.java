package com.veggofresh.delivery.dto.response;

import com.veggofresh.delivery.entity.PartnerTier;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DeliveryProfileStatsResponseDto {
    private long totalDeliveries;
    private double completionPercentage;
    private long daysActive;
    private Double averageRating;
    private long ratingCount;
    private PartnerTier tier;
}

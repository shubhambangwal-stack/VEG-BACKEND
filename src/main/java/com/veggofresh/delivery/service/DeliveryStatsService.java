package com.veggofresh.delivery.service;

import com.veggofresh.delivery.dto.response.DeliveryProfileStatsResponseDto;

import java.util.UUID;

public interface DeliveryStatsService {
    DeliveryProfileStatsResponseDto getStats(UUID deliveryPartnerUserId);
}

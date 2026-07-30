package com.veggofresh.delivery.service;

import com.veggofresh.delivery.dto.response.EarningsSummaryResponseDto;

import java.util.UUID;

public interface DeliveryEarningService {
    EarningsSummaryResponseDto getEarnings(UUID deliveryPartnerUserId, String period);
}

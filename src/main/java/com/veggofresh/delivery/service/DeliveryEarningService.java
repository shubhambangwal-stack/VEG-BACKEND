package com.veggofresh.delivery.service;

import com.veggofresh.delivery.dto.response.EarningsSummaryResponseDto;
import com.veggofresh.delivery.dto.response.EarningsTrendResponseDto;

import java.util.UUID;

public interface DeliveryEarningService {
    EarningsSummaryResponseDto getEarnings(UUID deliveryPartnerUserId, String period);

    /** Daily-bucketed totals for the last N days -- powers the Weekly Performance bar chart. */
    EarningsTrendResponseDto getTrend(UUID deliveryPartnerUserId, int days);
}

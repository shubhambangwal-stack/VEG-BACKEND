package com.veggofresh.delivery.controller;

import com.veggofresh.delivery.dto.response.EarningsSummaryResponseDto;
import com.veggofresh.delivery.dto.response.EarningsTrendResponseDto;
import com.veggofresh.delivery.service.DeliveryEarningService;
import com.veggofresh.platform.common.ApiResponse;
import com.veggofresh.platform.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@PreAuthorize("hasRole('DELIVERY')")
public class DeliveryEarningsController {

    private final DeliveryEarningService deliveryEarningService;

    @GetMapping("/api/delivery/earnings")
    public ResponseEntity<ApiResponse<EarningsSummaryResponseDto>> getEarnings(
            @RequestParam(required = false, defaultValue = "daily") String period) {
        var earnings = deliveryEarningService.getEarnings(SecurityUtils.getCurrentUserId(), period);
        return ResponseEntity.ok(ApiResponse.success(earnings, "Earnings retrieved successfully"));
    }

    @GetMapping("/api/delivery/earnings/trend")
    public ResponseEntity<ApiResponse<EarningsTrendResponseDto>> getTrend(
            @RequestParam(required = false, defaultValue = "7") int days) {
        var trend = deliveryEarningService.getTrend(SecurityUtils.getCurrentUserId(), days);
        return ResponseEntity.ok(ApiResponse.success(trend, "Earnings trend retrieved successfully"));
    }
}

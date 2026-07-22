package com.veggofresh.vendor.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class DashboardSummaryResponseDto {
    private String businessName;
    private Double todaysRevenue;
    private Double revenueChangePercent;
    private Integer activeOrdersCount;
    private Integer pendingPickupCount;
    private Integer outOfStockCount;
    private List<Integer> performanceTrend;
    private List<RecentOrderDto> recentOrders;
}

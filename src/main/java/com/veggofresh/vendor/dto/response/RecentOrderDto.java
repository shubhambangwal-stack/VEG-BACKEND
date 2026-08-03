package com.veggofresh.vendor.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RecentOrderDto {
    private String orderNumber;
    private String itemsSummary;
    private String timeAgo;
    private Double amount;
    private String status;
}

package com.veggofresh.delivery.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Powers the "Earnings Trend" daily bar chart on the Weekly Performance screen. */
@Getter
@Builder
public class EarningsTrendResponseDto {
    private BigDecimal totalForPeriod;
    private List<DailyEarningDto> days;

    @Getter
    @Builder
    public static class DailyEarningDto {
        private LocalDate date;
        private BigDecimal total;
        private long deliveryCount;
    }
}

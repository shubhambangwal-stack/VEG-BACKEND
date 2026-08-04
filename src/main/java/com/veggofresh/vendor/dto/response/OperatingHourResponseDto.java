package com.veggofresh.vendor.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.DayOfWeek;
import java.time.LocalTime;

@Getter
@Builder
public class OperatingHourResponseDto {
    private DayOfWeek dayOfWeek;
    private boolean isOpen;
    private LocalTime openTime;
    private LocalTime closeTime;
}

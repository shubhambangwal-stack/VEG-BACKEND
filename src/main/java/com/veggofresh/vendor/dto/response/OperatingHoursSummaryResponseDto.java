package com.veggofresh.vendor.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/** Figma "Operating Hours" screen, full state in one response. storeOnline reuses
 *  Shop.isOnline (the existing PUT /api/vendor/status toggle) rather than duplicating
 *  it -- Figma's "Store Status" toggle at the top of this screen is the same setting,
 *  just shown again in context. */
@Getter
@Builder
public class OperatingHoursSummaryResponseDto {
    private boolean storeOnline;
    private List<OperatingHourResponseDto> weeklySchedule;
    private List<SpecialClosureResponseDto> specialClosures;
}

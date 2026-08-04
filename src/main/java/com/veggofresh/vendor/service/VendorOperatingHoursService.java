package com.veggofresh.vendor.service;

import com.veggofresh.vendor.dto.request.OperatingHourUpdateRequestDto;
import com.veggofresh.vendor.dto.request.SpecialClosureRequestDto;
import com.veggofresh.vendor.dto.response.OperatingHoursSummaryResponseDto;
import com.veggofresh.vendor.dto.response.SpecialClosureResponseDto;

import java.util.List;
import java.util.UUID;

public interface VendorOperatingHoursService {
    /** Auto-creates a default weekly schedule (Mon-Fri 08:00-18:00 open, Sat/Sun closed) on first call. */
    OperatingHoursSummaryResponseDto getOperatingHours(UUID ownerUserId);

    /** Bulk save -- expects all 7 days in one call, matching the single "Save Changes" button. */
    OperatingHoursSummaryResponseDto updateOperatingHours(UUID ownerUserId, List<OperatingHourUpdateRequestDto> updates);

    SpecialClosureResponseDto addSpecialClosure(UUID ownerUserId, SpecialClosureRequestDto request);
    void deleteSpecialClosure(UUID ownerUserId, UUID closureId);
}

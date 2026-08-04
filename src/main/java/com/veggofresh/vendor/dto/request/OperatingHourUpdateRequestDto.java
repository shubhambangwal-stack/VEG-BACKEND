package com.veggofresh.vendor.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.DayOfWeek;
import java.time.LocalTime;

/** One entry in the bulk weekly-schedule save (Figma's single "Save Changes" button
 *  covers all 7 days at once, not a per-day save). openTime/closeTime only required
 *  when isOpen=true -- validated in the service, not here, since it's a cross-field rule. */
@Getter
@Setter
public class OperatingHourUpdateRequestDto {
    @NotNull(message = "Day of week is required")
    private DayOfWeek dayOfWeek;

    @NotNull(message = "isOpen is required")
    private Boolean isOpen;

    private LocalTime openTime;
    private LocalTime closeTime;
}

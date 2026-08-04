package com.veggofresh.vendor.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/** Figma "Add Holiday". Single-day closures just set startDate == endDate. */
@Getter
@Setter
public class SpecialClosureRequestDto {
    @NotBlank(message = "Closure name is required")
    private String name;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    private LocalDate endDate;
}

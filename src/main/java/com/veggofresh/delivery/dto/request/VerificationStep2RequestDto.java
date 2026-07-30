package com.veggofresh.delivery.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/** Figma "Vehicle Details, Step 2 of 3". Insurance PHOTO uploaded separately via
 *  POST /api/delivery/documents/INSURANCE -- this saves the structured fields. */
@Getter
@Setter
public class VerificationStep2RequestDto {
    @NotBlank(message = "Plate number is required")
    private String plateNumber;

    @NotBlank(message = "Vehicle model is required")
    private String vehicleModel;

    @NotNull(message = "Manufacture year is required")
    @Min(value = 1990, message = "Manufacture year looks invalid")
    private Integer manufactureYear;
}

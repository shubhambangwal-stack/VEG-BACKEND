package com.veggofresh.delivery.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/** Figma's signup form (name/city/vehicle type), captured right after OTP verification. */
@Getter
@Setter
public class BasicInfoRequestDto {
    @NotBlank(message = "Full name is required")
    private String fullName;

    @NotBlank(message = "City of operation is required")
    private String cityOfOperation;

    @NotBlank(message = "Vehicle type is required")
    private String vehicleType;
}

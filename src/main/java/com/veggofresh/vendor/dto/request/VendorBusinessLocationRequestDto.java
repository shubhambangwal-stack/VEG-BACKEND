package com.veggofresh.vendor.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/** Figma "Vendor Onboarding Step 2 of 3: Business Location". */
@Getter
@Setter
public class VendorBusinessLocationRequestDto {
    @NotBlank(message = "Street address is required")
    private String streetAddress;

    @NotBlank(message = "City is required")
    private String city;

    @NotBlank(message = "State/Province is required")
    private String state;

    @NotBlank(message = "ZIP/Postal code is required")
    private String zipCode;

    @NotNull(message = "Latitude is required (from the map pin)")
    private Double latitude;

    @NotNull(message = "Longitude is required (from the map pin)")
    private Double longitude;
}

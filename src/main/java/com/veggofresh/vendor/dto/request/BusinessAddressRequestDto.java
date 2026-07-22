package com.veggofresh.vendor.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class BusinessAddressRequestDto {
    @NotBlank(message = "Street is required")
    private String street;

    @NotBlank(message = "City is required")
    private String city;

    @NotBlank(message = "State is required")
    private String state;

    @NotBlank(message = "Zip code is required")
    private String zipCode;
}

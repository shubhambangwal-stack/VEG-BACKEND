package com.veggofresh.vendor.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.UUID;

@Data
public class ShopRegistrationRequestDto {
    private UUID ownerUserId; // Could also be extracted from JWT if integrating with Auth

    @NotBlank(message = "Shop name is required")
    private String name;

    @NotBlank(message = "Shop address is required")
    private String address;

    private Double latitude;
    private Double longitude;
}

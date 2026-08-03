package com.veggofresh.vendor.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/** Figma "Store Profile Management". */
@Getter
@Setter
public class StoreProfileRequestDto {
    @NotBlank(message = "Store name is required")
    private String storeName;

    private String storeBio;
    private String storeImageUrl;
    private List<String> attributes;

    @NotBlank(message = "Street address is required")
    private String streetAddress;

    @NotBlank(message = "City is required")
    private String city;

    @NotBlank(message = "ZIP/postal code is required")
    private String zipCode;

    private Double latitude;
    private Double longitude;
}

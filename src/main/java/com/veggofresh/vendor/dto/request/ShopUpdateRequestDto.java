package com.veggofresh.vendor.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ShopUpdateRequestDto {
    @NotBlank(message = "Shop name is required")
    private String name;

    @NotBlank(message = "Shop address is required")
    private String address;

    private Double latitude;
    private Double longitude;
}

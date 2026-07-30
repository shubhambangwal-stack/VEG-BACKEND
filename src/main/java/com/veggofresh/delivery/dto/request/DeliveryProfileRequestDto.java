package com.veggofresh.delivery.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DeliveryProfileRequestDto {
    @NotBlank(message = "Vehicle type is required")
    private String vehicleType;
}

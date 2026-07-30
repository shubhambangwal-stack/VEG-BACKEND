package com.veggofresh.delivery.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DeliveryLocationStatusRequestDto {
    @NotNull(message = "online flag is required")
    private Boolean online;
    private Double currentLatitude;
    private Double currentLongitude;
}

package com.veggofresh.customer.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class OrderRequestDto {

    @NotNull(message = "Address ID is required")
    private UUID addressId;
}

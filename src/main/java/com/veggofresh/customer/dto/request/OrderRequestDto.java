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

    /** UUID reference only — actual PaymentMethod entity lives in Payment module */
    private UUID paymentMethodId;

    /** Optional — delivery slot selected at checkout */
    private UUID deliverySlotId;

    /** Optional — ISO date string for scheduled delivery */
    private String scheduledDate;
}

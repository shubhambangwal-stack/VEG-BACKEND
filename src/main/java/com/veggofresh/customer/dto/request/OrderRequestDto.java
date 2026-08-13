package com.veggofresh.customer.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * PHASE 2 — one checkout call now processes ALL of the customer's open
 * carts at once, producing one Order per cart that still validates cleanly
 * (PROJECT_STATE section 2). There is deliberately no cartId field here:
 * checkout is all-or-nothing-per-cart across the whole basket, not a
 * per-cart action. Address/payment/slot apply uniformly to every resulting
 * order from this checkout.
 */
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

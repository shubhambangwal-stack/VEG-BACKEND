package com.veggofresh.customer.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * PHASE 2: the target cart is now supplied as a path variable on the
 * controller endpoint (/carts/{cartId}/promo-code), not in this body,
 * since a customer can have several open carts at once.
 */
@Getter
@Setter
public class PromoCodeRequestDto {
    @NotBlank(message = "Promo code is required")
    private String code;
}

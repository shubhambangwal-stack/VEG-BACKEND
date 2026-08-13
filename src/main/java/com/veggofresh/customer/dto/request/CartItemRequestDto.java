package com.veggofresh.customer.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * PHASE 2 note: unchanged shape — the multi-cart model deliberately keeps
 * this dumb (productId + quantity only). The system decides which cart the
 * item lands in or whether a new one is created; the client never picks a
 * cart when adding.
 */
@Getter
@Setter
public class CartItemRequestDto {

    @NotNull(message = "Product ID is required")
    private UUID productId;

    @Min(value = 1, message = "Quantity must be at least 1")
    private int quantity;
}

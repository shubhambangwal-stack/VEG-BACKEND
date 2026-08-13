package com.veggofresh.customer.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * PHASE 2 — one of possibly several open carts for the customer. Every
 * mutating cart endpoint now returns the full list of these (see
 * CustomerCartController) so the client can render "Cart 1 / Cart 2 / ..."
 * (PROJECT_STATE section 2).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartResponseDto {
    private UUID id;
    private UUID userId;
    /** Display label while shopping, e.g. "Cart 1" — derived from creation order. */
    private String cartLabel;
    private List<CartItemResponseDto> items;
    private BigDecimal totalAmount;
    private int itemCount;
    private BigDecimal deliveryFee;
    private BigDecimal estimatedTax;
    private String promoCode;
    private BigDecimal promoDiscount;
}

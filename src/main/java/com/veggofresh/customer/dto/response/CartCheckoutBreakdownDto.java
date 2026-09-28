package com.veggofresh.customer.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/** PHASE 2 — one cart's contribution to the multi-cart checkout summary. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartCheckoutBreakdownDto {
    private UUID cartId;
    /** Matches the cartLabel on the cart screen for the same cartId. */
    private String cartLabel;
    /**
     * Sum of line-item QUANTITIES that are still purchasable — identical to
     * {@code CartResponseDto.itemCount} for this cart. Previously this was
     * {@code getItems().size()}, i.e. the number of distinct line items, so a
     * cart of 2 lines at qty 2 and 5 read as 2 here and 7 on the cart screen.
     */
    private int itemCount;
    /** Line items in this cart that are no longer purchasable, and are not counted above. */
    private int unavailableItemCount;
    private BigDecimal subtotal;
    private BigDecimal deliveryFee;
    private BigDecimal estimatedTax;
    private BigDecimal promoDiscount;
    private String promoCode;
    private BigDecimal total;
}

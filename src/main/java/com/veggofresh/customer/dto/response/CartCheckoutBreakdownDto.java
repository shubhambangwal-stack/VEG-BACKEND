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
    private String cartLabel;
    private int itemCount;
    private BigDecimal subtotal;
    private BigDecimal deliveryFee;
    private BigDecimal estimatedTax;
    private BigDecimal promoDiscount;
    private String promoCode;
    private BigDecimal total;
}

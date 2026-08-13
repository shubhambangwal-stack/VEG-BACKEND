package com.veggofresh.customer.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * PHASE 2 — BREAKING CHANGE from the pre-pivot shape (which was one flat
 * subtotal/total for a single cart). Now a per-cart breakdown plus a grand
 * total, since one checkout call can produce N independent orders.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutSummaryDto {
    private List<CartCheckoutBreakdownDto> carts;
    private int totalItemCount;
    private BigDecimal grandTotal;
}

package com.veggofresh.customer.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * PHASE 2 — result of a multi-cart checkout: one payment can fan out into N
 * independent orders (PROJECT_STATE section 2). Any cart whose vendor
 * overlap broke between add-time and checkout-time is reported in
 * {@link #issues} and excluded from {@link #orders} rather than blocking
 * the rest of checkout.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutResultDto {
    private List<OrderResponseDto> orders;
    private List<CheckoutIssueDto> issues;
}

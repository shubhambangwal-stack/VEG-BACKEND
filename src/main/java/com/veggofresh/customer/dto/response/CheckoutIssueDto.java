package com.veggofresh.customer.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * PHASE 2 — reported when a cart's vendor overlap has broken between
 * add-time and checkout-time re-validation. That cart is excluded from the
 * resulting orders but does NOT block the rest of checkout
 * (PROJECT_STATE section 2, "Revisit-after-a-delay edge case").
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutIssueDto {
    private UUID cartId;
    private String cartLabel;
    private String reason;
}

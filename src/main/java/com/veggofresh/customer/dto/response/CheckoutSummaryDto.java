package com.veggofresh.customer.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutSummaryDto {
    private int itemCount;
    private BigDecimal subtotal;
    private BigDecimal deliveryFee;
    private BigDecimal estimatedTax;
    private BigDecimal promoDiscount;
    private String promoCode;
    private BigDecimal total;
}

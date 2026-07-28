package com.veggofresh.customer.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceDto {
    private String orderNumber;
    private String orderDate;          // formatted e.g. "Jul 28, 2026"
    private String customerName;       // from UserLookupService
    private String customerEmail;
    private String customerPhone;
    private String deliveryAddress;
    private List<InvoiceLineItemDto> items;
    private BigDecimal subtotal;
    private BigDecimal deliveryFee;
    private BigDecimal estimatedTax;
    private BigDecimal promoDiscount;
    private String promoCode;
    private BigDecimal total;
    private String paymentMethod;      // display label, e.g. "Credit Card", "COD"
}

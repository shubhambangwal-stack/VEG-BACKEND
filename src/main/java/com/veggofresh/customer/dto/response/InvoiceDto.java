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
    private String orderDate;
    private String customerName;
    private String customerEmail;
    private String customerPhone;

    /** Seller of record -- null until a shop has accepted this order. */
    private String shopName;
    private String shopAddress;

    private String deliveryAddress;
    private List<InvoiceLineItemDto> items;
    private BigDecimal subtotal;
    private BigDecimal deliveryFee;
    private BigDecimal estimatedTax;
    private BigDecimal promoDiscount;
    private String promoCode;
    private BigDecimal total;
    private String paymentMethod;
}

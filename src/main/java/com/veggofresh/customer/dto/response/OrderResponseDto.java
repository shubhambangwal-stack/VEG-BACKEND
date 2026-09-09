package com.veggofresh.customer.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponseDto {
    private UUID id;
    private UUID userId;
    private String orderNumber;
    private String status;
    private BigDecimal totalAmount;
    private BigDecimal deliveryFee;
    private BigDecimal estimatedTax;
    private BigDecimal promoDiscount;
    private String promoCode;
    private String deliveryAddress;
    private double latitude;
    private double longitude;
    private String scheduledDate;
    private String deliveryTimeSlot;
    private String paymentMethod;
    private int itemCount;
    private List<String> itemThumbnails;
    private String estimatedDeliveryWindow;
    private boolean canTrack;
    private boolean canReorder;
    private boolean canCancel;
    private List<OrderItemResponseDto> items;
    private Instant createdAt;
    private Instant updatedAt;

    /**
     * The customer's own display name -- always resolvable regardless of order
     * state (it's the customer's own data), used by Vendor/Delivery's own
     * gated views of this same DTO (see OrderResponseMapper).
     */
    private String customerName;

    /**
     * Vendor identity -- null until a shop has actually accepted this order
     * (order.acceptedShopId set). Not the owner's personal phone -- this is
     * the shop's own business contact number (see ShopLookupService).
     */
    private String shopName;
    private String shopBusinessPhone;

    /**
     * Delivery partner identity -- null until a partner has accepted the
     * dispatched assignment (Order.deliveryAgentName/Phone, populated by
     * CustomerOrderService.assignDeliveryAgent()).
     */
    private String deliveryAgentName;
    private String deliveryAgentPhone;

    /**
     * Vendor's estimated payout for this order (product subtotal minus the
     * admin-configured platform commission). Only ever populated by
     * VendorOrderManagementService after calling the shared mapper -- left
     * null on every customer-facing response since it's not the customer's
     * concern. See PlatformSettingsService.getPlatformCommissionPercent().
     */
    private java.math.BigDecimal estimatedPayout;
}

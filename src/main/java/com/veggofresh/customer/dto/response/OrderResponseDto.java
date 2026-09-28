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
    /**
     * The cart this order was built from, when it came from a cart. One checkout
     * call produces N orders, one per cart, and the client labels each cart
     * "Cart 1", "Cart 2", ...; without this the response gives the customer N
     * orders and no way to tell which cart each one came from, so "Cart 2 was
     * ordered" cannot be reconciled with the order that actually happened.
     *
     * <p>Null for orders not created from a cart.
     */
    private UUID sourceCartId;
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

    /**
     * Deadline by which a vendor must accept/reject this order request before
     * the {@code VendorAcceptTimeoutSweepService} auto-cancels it. Computed as
     * {@code order.createdAt + PlatformSettingsService.getVendorAcceptTimeoutSeconds()}
     * (admin-configurable). Only ever populated by
     * {@code CustomerOrderServiceImpl.getOrderRequestsForShop()} -- i.e. only on
     * the vendor's still-live "order requests" broadcast list, since that's the
     * only place this deadline is actionable. Left null everywhere else
     * (customer's own view, a shop's already-accepted order history, etc.).
     */
    private Instant vendorAcceptExpiresAt;
}

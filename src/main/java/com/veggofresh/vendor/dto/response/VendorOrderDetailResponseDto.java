package com.veggofresh.vendor.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Figma "Order Details" screen. IMPORTANT: subtotal/serviceFee/total here are scoped to
 * THIS SHOP's items only, not the full order -- a single Customer order could span
 * multiple vendors' products (nothing scopes a cart to one shop), so 'items' is
 * filtered down before these totals are computed.
 */
@Getter
@Builder
public class VendorOrderDetailResponseDto {
    private UUID orderId;
    private String status;
    private List<VendorOrderItemDto> items;

    private String customerPhone;
    private String deliveryAddress;
    private Double latitude;
    private Double longitude;

    /** Sum of this shop's items only. */
    private BigDecimal subtotal;

    /** Flat placeholder rate -- no real fee engine exists anywhere. See NOTES_VENDOR.md. */
    private BigDecimal serviceFeePercent;
    private BigDecimal serviceFee;
    private BigDecimal totalForThisShop;

    private Instant createdAt;
    private Instant updatedAt;
}

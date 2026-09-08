package com.veggofresh.customer.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Minimal cross-module DTO used by the Delivery module to compute wallet settlement
 * after a delivery completes. Carries only the fields from {@code Order} that the
 * settlement calculation needs -- Delivery never imports the Order @Entity directly.
 *
 * Fields:
 * <ul>
 *   <li>{@code totalAmount}   -- full order total (subtotal + deliveryFee + tax), as charged to customer</li>
 *   <li>{@code deliveryFee}   -- delivery fee component of totalAmount (what the customer was quoted)</li>
 *   <li>{@code estimatedTax}  -- tax component (may be null/zero if tax not tracked)</li>
 *   <li>{@code acceptedShopId}-- the vendor shop that accepted this order; used to resolve the vendor user id</li>
 * </ul>
 *
 * Note: orderSubtotal (basis for platform commission) = totalAmount - deliveryFee - estimatedTax.
 * The delivery partner's actual earning is computed separately from the EarningRecord formula
 * (BASE_PAY + distanceFare) -- not from deliveryFee -- since those two numbers can differ.
 */
@Getter
@Builder
public class OrderSettlementDto {

    private UUID orderId;

    /** Full amount charged to the customer (subtotal + deliveryFee + tax). */
    private BigDecimal totalAmount;

    /** Delivery fee component shown on the order receipt. May be null if not set. */
    private BigDecimal deliveryFee;

    /** Estimated tax component. May be null. */
    private BigDecimal estimatedTax;

    /**
     * The vendor shop that won the accept race for this order.
     * Use {@code ShopLookupService.findOwnerUserIdByShopId()} to resolve to a user id.
     * May be null if order was never accepted (should not happen at delivery-complete time).
     */
    private UUID acceptedShopId;
}

package com.veggofresh.delivery.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Cross-module DTO -- consumed by Vendor's order-detail view once a "ready for pickup" order has been dispatched. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VendorDeliveryStatusDto {
    /** false if dispatchOrder() was never called for this order -- shouldn't happen once wired, but defensive. */
    private boolean dispatched;

    /** DeliveryAssignmentStatus name of the current (most recent round's) assignment -- PENDING, ACCEPTED, ARRIVED_AT_STORE, PICKED_UP, ARRIVED_AT_DROP, DELIVERED, or a terminal failure state. Null if !dispatched. */
    private String status;

    /** Null until a partner has accepted (status still PENDING). */
    private String partnerName;
    private String partnerPhone;
}

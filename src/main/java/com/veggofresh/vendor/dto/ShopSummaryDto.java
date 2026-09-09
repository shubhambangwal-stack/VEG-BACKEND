package com.veggofresh.vendor.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

/**
 * Cross-module-safe shop summary -- used by Customer and Delivery to show
 * "who is this vendor" (name, business contact number, address) once the
 * viewer is actually entitled to see it (order accepted by this shop / this
 * delivery partner has accepted the assignment). Never expose the {@code Shop}
 * @Entity itself outside the Vendor module -- this DTO is the boundary.
 */
@Getter
@Builder
public class ShopSummaryDto {
    private UUID shopId;
    private String name;
    private String businessPhone;
    private String address;
}

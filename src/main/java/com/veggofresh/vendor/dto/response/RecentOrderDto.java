package com.veggofresh.vendor.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@Builder
public class RecentOrderDto {
    /**
     * Real order id -- pass this to GET /api/vendor/orders/{id} for the order
     * detail screen. Previously missing entirely; orderNumber below was NOT
     * usable for this (it was a truncated UUID substring, not this id).
     */
    private UUID id;

    /** The order's real, human-facing order number (Order.orderNumber). */
    private String orderNumber;

    private String itemsSummary;
    private String timeAgo;
    private Double amount;
    private String status;

    /**
     * Up to 3 product image URLs, same source (CatalogProduct.imageUrl via
     * Cloudinary) and same shape as OrderResponseDto.itemThumbnails used
     * everywhere else -- real per-order data, not a placeholder.
     */
    private List<String> itemThumbnails;
}

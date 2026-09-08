package com.veggofresh.vendor.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Cross-module DTO for product subcategories, scoped to a parent category.
 * Exposed via ProductCatalogService for the Customer module to consume --
 * mirrors CategoryDto exactly, one level down the taxonomy.
 *
 * GAP FIX: imageUrl added -- this DTO had no image field at all before,
 * so the customer-facing subcategory picker could never show one even
 * though Admin's SubcategoryResponseDto (the actual source) already had it.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubcategoryDto {
    private UUID id;
    private UUID categoryId;
    private String categoryName;
    private String name;
    private String imageUrl;
    private boolean isActive;
}

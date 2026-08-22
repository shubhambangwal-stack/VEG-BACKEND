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
    private boolean isActive;
}

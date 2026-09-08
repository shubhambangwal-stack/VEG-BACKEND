package com.veggofresh.admin.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * NEW -- one category with its subcategories and each subcategory's products
 * nested inside, for Admin's combined "get everything in one go" endpoints:
 * GET /api/admin/catalog/categories/{categoryId}/tree and
 * GET /api/admin/catalog/tree.
 *
 * No filtering of any kind -- raw structure exactly as stored, matching
 * Admin's existing flat endpoints (which also apply no radius/listed logic).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryTreeDto {
    private UUID id;
    private String name;
    private String description;
    private String imageUrl;
    private List<SubcategoryTreeDto> subcategories;
}

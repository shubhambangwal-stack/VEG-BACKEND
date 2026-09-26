package com.veggofresh.vendor.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * NEW -- one category with its subcategories (and each subcategory's eligible
 * products) nested inside, for Customer's combined "get everything in one go"
 * endpoints: GET /api/customer/categories/{categoryId}/tree and
 * GET /api/customer/categories/tree.
 *
 * Same radius/eligibility rules as every other Customer product-facing
 * endpoint apply to the nested products (see ProductCatalogServiceImpl) --
 * categories/subcategories themselves are NOT radius-filtered, matching
 * browseCategories()/browseSubcategories() today.
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

package com.veggofresh.vendor.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * NEW -- one category with its subcategories (and each subcategory's products,
 * with this vendor's own isListed state merged in) nested inside, for Vendor's
 * combined "get everything in one go" endpoints: GET /api/vendor/categories/{id}/tree
 * and GET /api/vendor/categories/tree.
 *
 * No radius filtering, no listed-only filtering -- shows the entire Admin
 * catalog, exactly matching today's GET /api/vendor/listings semantics
 * ("browse to decide what to add"), just nested instead of flat.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VendorCategoryTreeDto {
    private UUID id;
    private String name;
    private String description;
    private String imageUrl;
    private List<VendorSubcategoryTreeDto> subcategories;
}

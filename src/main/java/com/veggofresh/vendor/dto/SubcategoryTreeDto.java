package com.veggofresh.vendor.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/** NEW -- see CategoryTreeDto. products is radius-filtered/eligibility-filtered
 *  the same way ProductCatalogService.searchProducts() already is. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubcategoryTreeDto {
    private UUID id;
    private String name;
    private String imageUrl;
    private List<ProductDto> products;
}

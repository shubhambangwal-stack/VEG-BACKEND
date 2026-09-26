package com.veggofresh.admin.service;

import com.veggofresh.admin.dto.response.CategoryTreeDto;

import java.util.List;
import java.util.UUID;

/**
 * NEW -- composes CatalogCategoryService + CatalogSubcategoryService +
 * AdminProductService into one nested response, for Admin's combined
 * "get everything in one go" endpoints. No filtering of any kind -- raw
 * structure exactly as stored.
 */
public interface CatalogTreeService {

    /** One category with its subcategories and each subcategory's products nested inside. */
    CategoryTreeDto getCategoryTree(UUID categoryId);

    /**
     * The entire catalog, every category with every subcategory with every
     * product nested inside. Testing/tooling use, not the production app --
     * cost scales with total catalog size.
     */
    List<CategoryTreeDto> getFullCatalogTree();
}

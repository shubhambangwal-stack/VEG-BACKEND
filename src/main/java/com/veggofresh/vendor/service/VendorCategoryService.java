package com.veggofresh.vendor.service;

import com.veggofresh.admin.dto.response.CategoryResponseDto;
import com.veggofresh.admin.dto.response.SubcategoryResponseDto;
import com.veggofresh.vendor.dto.response.VendorCategoryTreeDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

/**
 * Read-only, vendor-facing view onto Admin's category/subcategory taxonomy.
 * A vendor never creates or edits a category -- this exists purely so the
 * frontend can populate a searchable category/subcategory picker with real
 * ids, instead of the vendor ever having to know a raw UUID.
 *
 * Delegates to Admin's CatalogCategoryService / CatalogSubcategoryService
 * (cross-module interfaces, same pattern as AdminProductService is already
 * used inside VendorListingServiceImpl) -- no admin entities are touched
 * directly, and no admin data is duplicated.
 */
public interface VendorCategoryService {

    /** Paginated, searchable, active-only categories. */
    Page<CategoryResponseDto> browseCategories(String search, Pageable pageable);

    /** Paginated, searchable, active-only subcategories under one category. */
    Page<SubcategoryResponseDto> browseSubcategories(UUID categoryId, String search, Pageable pageable);

    /**
     * NEW -- one category with its subcategories and each subcategory's products
     * (this vendor's own isListed state merged in), all nested in one response.
     * No radius filtering, no listed-only filtering -- entire Admin catalog,
     * same semantics as browseCatalog()/getMyListings() today, just nested.
     */
    VendorCategoryTreeDto getCategoryTree(UUID ownerUserId, UUID categoryId);

    /**
     * NEW -- the entire active catalog, every category with every subcategory
     * with every product nested inside (isListed merged in per product).
     * Intended for testing/tooling, not the production mobile app -- cost
     * scales with total catalog size.
     */
    List<VendorCategoryTreeDto> getFullCatalogTree(UUID ownerUserId);
}

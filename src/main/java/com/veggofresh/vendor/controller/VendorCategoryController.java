package com.veggofresh.vendor.controller;

import com.veggofresh.admin.dto.response.CategoryResponseDto;
import com.veggofresh.admin.dto.response.SubcategoryResponseDto;
import com.veggofresh.platform.common.ApiResponse;
import com.veggofresh.platform.common.PageResponse;
import com.veggofresh.platform.security.SecurityUtils;
import com.veggofresh.vendor.dto.response.VendorCategoryTreeDto;
import com.veggofresh.vendor.service.VendorCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Read-only category/subcategory picker for the vendor frontend, so a vendor
 * can search "Vegetables" by name and get a real categoryId back to use in
 * GET /api/vendor/listings?categoryId=&subcategoryId= -- they never need to
 * know or type a UUID themselves.
 *
 * <pre>
 * GET /api/vendor/categories?search=&page=&size=
 * GET /api/vendor/categories/{categoryId}/subcategories?search=&page=&size=
 * GET /api/vendor/categories/{categoryId}/tree
 *     — NEW: this category, its subcategories, and each subcategory's products
 *       (this vendor's isListed merged in), all nested in one response.
 * GET /api/vendor/categories/tree
 *     — NEW: the entire catalog, same nesting, every category at once.
 *       Testing/tooling use, not the production app -- cost scales with catalog size.
 * </pre>
 *
 * Vendor never creates/edits categories -- that stays exclusively in
 * AdminCatalogController (hasRole('ADMIN')). This controller only reads.
 */
@RestController
@RequestMapping("/api/vendor/categories")
@RequiredArgsConstructor
@PreAuthorize("hasRole('VENDOR')")
public class VendorCategoryController {

    private final VendorCategoryService vendorCategoryService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<CategoryResponseDto>>> browseCategories(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<CategoryResponseDto> result = vendorCategoryService.browseCategories(
                search, PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "displayOrder")));
        return ResponseEntity.ok(ApiResponse.success(PageResponse.of(result), "Categories retrieved successfully"));
    }

    @GetMapping("/{categoryId}/subcategories")
    public ResponseEntity<ApiResponse<PageResponse<SubcategoryResponseDto>>> browseSubcategories(
            @PathVariable UUID categoryId,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<SubcategoryResponseDto> result = vendorCategoryService.browseSubcategories(
                categoryId, search, PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "displayOrder")));
        return ResponseEntity.ok(ApiResponse.success(PageResponse.of(result), "Subcategories retrieved successfully"));
    }

    /** NEW -- one category, nested subcategories + products, isListed merged in per product. */
    @GetMapping("/{categoryId}/tree")
    public ResponseEntity<ApiResponse<VendorCategoryTreeDto>> getCategoryTree(@PathVariable UUID categoryId) {
        VendorCategoryTreeDto result = vendorCategoryService.getCategoryTree(SecurityUtils.getCurrentUserId(), categoryId);
        return ResponseEntity.ok(ApiResponse.success(result, "Category tree retrieved successfully"));
    }

    /** NEW -- entire catalog, same nesting. Testing/tooling use -- cost scales with catalog size. */
    @GetMapping("/tree")
    public ResponseEntity<ApiResponse<List<VendorCategoryTreeDto>>> getFullCatalogTree() {
        List<VendorCategoryTreeDto> result = vendorCategoryService.getFullCatalogTree(SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success(result, "Full catalog tree retrieved successfully"));
    }
}

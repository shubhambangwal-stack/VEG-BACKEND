package com.veggofresh.admin.controller;

import com.veggofresh.admin.dto.request.CategoryRequestDto;
import com.veggofresh.admin.dto.request.ProductRequestDto;
import com.veggofresh.admin.dto.request.SubcategoryRequestDto;
import com.veggofresh.admin.dto.response.CategoryResponseDto;
import com.veggofresh.admin.dto.response.ProductResponseDto;
import com.veggofresh.admin.dto.response.SubcategoryResponseDto;
import com.veggofresh.admin.service.AdminProductService;
import com.veggofresh.admin.service.CatalogCategoryService;
import com.veggofresh.admin.service.CatalogSubcategoryService;
import com.veggofresh.platform.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Admin Master Product Catalog Controller (Phase 1)
 *
 * <pre>
 * ── Categories ───────────────────────────────────────────────
 * GET    /api/admin/catalog/categories?includeInactive=
 * POST   /api/admin/catalog/categories
 * GET    /api/admin/catalog/categories/{id}
 * PUT    /api/admin/catalog/categories/{id}
 * PATCH  /api/admin/catalog/categories/{id}/status?active=
 *
 * ── Subcategories ────────────────────────────────────────────
 * GET    /api/admin/catalog/subcategories?categoryId=
 * POST   /api/admin/catalog/subcategories
 * GET    /api/admin/catalog/subcategories/{id}
 * PUT    /api/admin/catalog/subcategories/{id}
 * PATCH  /api/admin/catalog/subcategories/{id}/status?active=
 *
 * ── Products ─────────────────────────────────────────────────
 * GET    /api/admin/catalog/products?search=&categoryId=&subcategoryId=&page=&size=
 * POST   /api/admin/catalog/products
 * GET    /api/admin/catalog/products/{id}
 * PUT    /api/admin/catalog/products/{id}
 * PATCH  /api/admin/catalog/products/{id}/status?active=
 *
 * No hard-delete endpoints anywhere in this controller, deliberately —
 * see NOTES_ADMIN.md, "Catalog: no hard delete."
 * </pre>
 */
@RestController
@RequestMapping("/api/admin/catalog")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminCatalogController {

    private final CatalogCategoryService categoryService;
    private final CatalogSubcategoryService subcategoryService;
    private final AdminProductService adminProductService;

    // ── CATEGORIES ───────────────────────────────────────────────────────

    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<CategoryResponseDto>>> listCategories(
            @RequestParam(defaultValue = "false") boolean includeInactive) {
        return ResponseEntity.ok(ApiResponse.success(
                categoryService.listCategories(includeInactive), "Categories retrieved successfully"));
    }

    @PostMapping("/categories")
    public ResponseEntity<ApiResponse<CategoryResponseDto>> createCategory(
            @Valid @RequestBody CategoryRequestDto request) {
        return ResponseEntity.ok(ApiResponse.success(
                categoryService.createCategory(request), "Category created successfully"));
    }

    @GetMapping("/categories/{id}")
    public ResponseEntity<ApiResponse<CategoryResponseDto>> getCategory(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(
                categoryService.getCategoryById(id), "Category retrieved successfully"));
    }

    @PutMapping("/categories/{id}")
    public ResponseEntity<ApiResponse<CategoryResponseDto>> updateCategory(
            @PathVariable UUID id, @Valid @RequestBody CategoryRequestDto request) {
        return ResponseEntity.ok(ApiResponse.success(
                categoryService.updateCategory(id, request), "Category updated successfully"));
    }

    @PatchMapping("/categories/{id}/status")
    public ResponseEntity<ApiResponse<CategoryResponseDto>> setCategoryStatus(
            @PathVariable UUID id, @RequestParam boolean active) {
        return ResponseEntity.ok(ApiResponse.success(
                categoryService.setActive(id, active),
                active ? "Category activated successfully" : "Category deactivated successfully"));
    }

    // ── SUBCATEGORIES ────────────────────────────────────────────────────

    @GetMapping("/subcategories")
    public ResponseEntity<ApiResponse<List<SubcategoryResponseDto>>> listSubcategories(
            @RequestParam UUID categoryId) {
        return ResponseEntity.ok(ApiResponse.success(
                subcategoryService.listByCategory(categoryId), "Subcategories retrieved successfully"));
    }

    @PostMapping("/subcategories")
    public ResponseEntity<ApiResponse<SubcategoryResponseDto>> createSubcategory(
            @Valid @RequestBody SubcategoryRequestDto request) {
        return ResponseEntity.ok(ApiResponse.success(
                subcategoryService.createSubcategory(request), "Subcategory created successfully"));
    }

    @GetMapping("/subcategories/{id}")
    public ResponseEntity<ApiResponse<SubcategoryResponseDto>> getSubcategory(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(
                subcategoryService.getSubcategoryById(id), "Subcategory retrieved successfully"));
    }

    @PutMapping("/subcategories/{id}")
    public ResponseEntity<ApiResponse<SubcategoryResponseDto>> updateSubcategory(
            @PathVariable UUID id, @Valid @RequestBody SubcategoryRequestDto request) {
        return ResponseEntity.ok(ApiResponse.success(
                subcategoryService.updateSubcategory(id, request), "Subcategory updated successfully"));
    }

    @PatchMapping("/subcategories/{id}/status")
    public ResponseEntity<ApiResponse<SubcategoryResponseDto>> setSubcategoryStatus(
            @PathVariable UUID id, @RequestParam boolean active) {
        return ResponseEntity.ok(ApiResponse.success(
                subcategoryService.setActive(id, active),
                active ? "Subcategory activated successfully" : "Subcategory deactivated successfully"));
    }

    // ── PRODUCTS ─────────────────────────────────────────────────────────

    @GetMapping("/products")
    public ResponseEntity<ApiResponse<Page<ProductResponseDto>>> searchProducts(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) UUID subcategoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(ApiResponse.success(
                adminProductService.searchProducts(search, categoryId, subcategoryId, pageable),
                "Products retrieved successfully"));
    }

    @PostMapping("/products")
    public ResponseEntity<ApiResponse<ProductResponseDto>> createProduct(
            @Valid @RequestBody ProductRequestDto request) {
        return ResponseEntity.ok(ApiResponse.success(
                adminProductService.createProduct(request), "Product created successfully"));
    }

    @GetMapping("/products/{id}")
    public ResponseEntity<ApiResponse<ProductResponseDto>> getProduct(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(
                adminProductService.getProductById(id), "Product retrieved successfully"));
    }

    @PutMapping("/products/{id}")
    public ResponseEntity<ApiResponse<ProductResponseDto>> updateProduct(
            @PathVariable UUID id, @Valid @RequestBody ProductRequestDto request) {
        return ResponseEntity.ok(ApiResponse.success(
                adminProductService.updateProduct(id, request), "Product updated successfully"));
    }

    @PatchMapping("/products/{id}/status")
    public ResponseEntity<ApiResponse<ProductResponseDto>> setProductStatus(
            @PathVariable UUID id, @RequestParam boolean active) {
        return ResponseEntity.ok(ApiResponse.success(
                adminProductService.setActive(id, active),
                active ? "Product activated successfully" : "Product deactivated successfully"));
    }
}
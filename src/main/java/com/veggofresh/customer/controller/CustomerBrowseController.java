package com.veggofresh.customer.controller;

import com.veggofresh.platform.common.ApiResponse;
import com.veggofresh.platform.common.PageResponse;
import com.veggofresh.vendor.dto.CategoryDto;
import com.veggofresh.vendor.dto.ProductDto;
import com.veggofresh.vendor.dto.ShopDto;
import com.veggofresh.vendor.service.ProductCatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/customer")
@RequiredArgsConstructor
public class CustomerBrowseController {

    private final ProductCatalogService productCatalogService;

    @GetMapping("/shops")
    public ResponseEntity<ApiResponse<List<ShopDto>>> browseShops(
            @RequestParam double latitude,
            @RequestParam double longitude) {
        List<ShopDto> shops = productCatalogService.browseNearbyShops(latitude, longitude);
        return ResponseEntity.ok(ApiResponse.success(shops, "Shops retrieved successfully"));
    }

    @GetMapping("/products")
    public ResponseEntity<ApiResponse<PageResponse<ProductDto>>> searchProducts(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<ProductDto> products = productCatalogService.searchProducts(query, category, minPrice, maxPrice, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(PageResponse.of(products), "Products retrieved successfully"));
    }

    @GetMapping("/products/{id}")
    public ResponseEntity<ApiResponse<ProductDto>> getProduct(@PathVariable UUID id) {
        ProductDto product = productCatalogService.getProductById(id);
        return ResponseEntity.ok(ApiResponse.success(product, "Product retrieved successfully"));
    }

    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<CategoryDto>>> getCategories() {
        List<CategoryDto> categories = productCatalogService.getAllCategories();
        return ResponseEntity.ok(ApiResponse.success(categories, "Categories retrieved successfully"));
    }

    @GetMapping("/products/{id}/related")
    public ResponseEntity<ApiResponse<List<ProductDto>>> getRelatedProducts(@PathVariable UUID id) {
        List<ProductDto> related = productCatalogService.getRelatedProducts(id);
        return ResponseEntity.ok(ApiResponse.success(related, "Related products retrieved successfully"));
    }

    @GetMapping("/products/deals")
    public ResponseEntity<ApiResponse<List<ProductDto>>> getDailyDeals() {
        List<ProductDto> deals = productCatalogService.getDailyDeals();
        return ResponseEntity.ok(ApiResponse.success(deals, "Daily deals retrieved successfully"));
    }

    @GetMapping("/banners")
    public ResponseEntity<ApiResponse<List<Map<String, String>>>> getBanners() {
        List<Map<String, String>> banners = List.of(
                Map.of("id", UUID.randomUUID().toString(), "title", "Fresh Organic Veggies", "subtitle", "Get up to 20% off today", "imageUrl", "https://veggofresh.com/banners/organic.png"),
                Map.of("id", UUID.randomUUID().toString(), "title", "Juicy Summer Fruits", "subtitle", "Flat 10% discount", "imageUrl", "https://veggofresh.com/banners/fruits.png")
        );
        return ResponseEntity.ok(ApiResponse.success(banners, "Hero banners retrieved successfully"));
    }
}

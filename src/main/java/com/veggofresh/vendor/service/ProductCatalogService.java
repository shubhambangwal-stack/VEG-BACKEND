package com.veggofresh.vendor.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.veggofresh.vendor.dto.CategoryDto;
import com.veggofresh.vendor.dto.ProductDto;
import com.veggofresh.vendor.dto.ShopDto;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Interface contract provided by the Vendor module for the Customer module to consume.
 * Cross-module calls must ONLY go through this interface — never import Vendor @Entity directly.
 */
public interface ProductCatalogService {
    List<ShopDto> browseNearbyShops(double latitude, double longitude);
    Page<ProductDto> searchProducts(String query, String category, Double minPrice, Double maxPrice, Pageable pageable);
    ProductDto getProductById(UUID productId);
    List<ProductDto> getAvailableProducts(UUID shopId);

    // GAP 8 — new methods needed by Customer module
    /** Returns all product categories with icon URLs */
    List<CategoryDto> getAllCategories();

    /** Returns products related to the given product (same category) */
    List<ProductDto> getRelatedProducts(UUID productId);

    /** Returns products with active discounts (deals of the day) */
    List<ProductDto> getDailyDeals();
}

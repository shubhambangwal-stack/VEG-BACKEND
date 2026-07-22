package com.veggofresh.vendor.service;

import com.veggofresh.vendor.dto.ShopDto;
import com.veggofresh.vendor.dto.ProductDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Interface contract provided by the Vendor module for the Customer module to consume.
 */
public interface ProductCatalogService {
    List<ShopDto> browseNearbyShops(double latitude, double longitude);
    Page<ProductDto> searchProducts(String query, String category, Double minPrice, Double maxPrice, Pageable pageable);
    ProductDto getProductById(UUID productId);
    List<ProductDto> getAvailableProducts(UUID shopId);
}

package com.veggofresh.vendor.service;

import com.veggofresh.vendor.dto.response.ProductDto;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Interface contract provided by the Vendor module for the Customer module to consume.
 */
public interface ProductCatalogService {
    Optional<ProductDto> getProductById(UUID productId);
    List<ProductDto> getAvailableProducts(UUID shopId);
}

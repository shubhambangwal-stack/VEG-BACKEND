package com.veggofresh.admin.service;

import com.veggofresh.admin.dto.request.ProductRequestDto;
import com.veggofresh.admin.dto.response.ProductResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface AdminProductService {
    ProductResponseDto createProduct(ProductRequestDto request);
    ProductResponseDto updateProduct(UUID id, ProductRequestDto request);
    ProductResponseDto getProductById(UUID id);
    java.util.Optional<ProductResponseDto> findProductById(UUID id);
    Page<ProductResponseDto> searchProducts(String search, UUID categoryId, UUID subcategoryId, Pageable pageable);
    ProductResponseDto setActive(UUID id, boolean active);
}
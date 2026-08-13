package com.veggofresh.admin.service.impl;

import com.veggofresh.admin.dto.request.ProductRequestDto;
import com.veggofresh.admin.dto.response.ProductResponseDto;
import com.veggofresh.admin.entity.CatalogCategory;
import com.veggofresh.admin.entity.CatalogProduct;
import com.veggofresh.admin.entity.CatalogSubcategory;
import com.veggofresh.admin.repository.CatalogCategoryRepository;
import com.veggofresh.admin.repository.CatalogProductRepository;
import com.veggofresh.admin.repository.CatalogSubcategoryRepository;
import com.veggofresh.admin.service.AdminProductService;
import com.veggofresh.platform.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminProductServiceImpl implements AdminProductService {

    private final CatalogProductRepository productRepository;
    private final CatalogCategoryRepository categoryRepository;
    private final CatalogSubcategoryRepository subcategoryRepository;

    @Override
    public ProductResponseDto createProduct(ProductRequestDto request) {
        CatalogCategory category = getCategory(request.getCategoryId());
        CatalogSubcategory subcategory = getSubcategory(request.getSubcategoryId());
        validateSubcategoryBelongsToCategory(subcategory, category);

        if (productRepository.existsByNameIgnoreCaseAndSubcategoryId(request.getName(), subcategory.getId())) {
            throw new BusinessException("CATALOG_PRODUCT_DUPLICATE",
                    "A product with this name already exists in this subcategory", HttpStatus.CONFLICT);
        }

        CatalogProduct product = new CatalogProduct();
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setCategory(category);
        product.setSubcategory(subcategory);
        product.setPrice(request.getPrice());
        product.setImageUrl(request.getImageUrl());
        product.setQuantityUnit(request.getQuantityUnit());
        product.setQuantityValue(request.getQuantityValue());
        product.setActive(true);
        return toDto(productRepository.save(product));
    }

    @Override
    public ProductResponseDto updateProduct(UUID id, ProductRequestDto request) {
        CatalogProduct product = getEntity(id);
        CatalogCategory category = getCategory(request.getCategoryId());
        CatalogSubcategory subcategory = getSubcategory(request.getSubcategoryId());
        validateSubcategoryBelongsToCategory(subcategory, category);

        boolean nameOrSubcategoryChanged = !product.getName().equalsIgnoreCase(request.getName())
                || !product.getSubcategory().getId().equals(subcategory.getId());
        if (nameOrSubcategoryChanged
                && productRepository.existsByNameIgnoreCaseAndSubcategoryId(request.getName(), subcategory.getId())) {
            throw new BusinessException("CATALOG_PRODUCT_DUPLICATE",
                    "A product with this name already exists in this subcategory", HttpStatus.CONFLICT);
        }

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setCategory(category);
        product.setSubcategory(subcategory);
        product.setPrice(request.getPrice());
        product.setImageUrl(request.getImageUrl());
        product.setQuantityUnit(request.getQuantityUnit());
        product.setQuantityValue(request.getQuantityValue());
        return toDto(productRepository.save(product));
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponseDto getProductById(UUID id) {
        return toDto(getEntity(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponseDto> searchProducts(String search, UUID categoryId, UUID subcategoryId, Pageable pageable) {
        String normalizedSearch = (search != null && !search.isBlank()) ? search.trim() : null;
        return productRepository.search(normalizedSearch, categoryId, subcategoryId, pageable)
                .map(this::toDto);
    }

    @Override
    public ProductResponseDto setActive(UUID id, boolean active) {
        CatalogProduct product = getEntity(id);
        product.setActive(active);
        return toDto(productRepository.save(product));
    }

    private void validateSubcategoryBelongsToCategory(CatalogSubcategory subcategory, CatalogCategory category) {
        if (!subcategory.getCategory().getId().equals(category.getId())) {
            throw new BusinessException("CATALOG_SUBCATEGORY_CATEGORY_MISMATCH",
                    "Subcategory does not belong to the given category", HttpStatus.BAD_REQUEST);
        }
    }

    private CatalogProduct getEntity(UUID id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new BusinessException("CATALOG_PRODUCT_NOT_FOUND",
                        "Product not found", HttpStatus.NOT_FOUND));
    }

    private CatalogCategory getCategory(UUID categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new BusinessException("CATALOG_CATEGORY_NOT_FOUND",
                        "Category not found", HttpStatus.NOT_FOUND));
    }

    private CatalogSubcategory getSubcategory(UUID subcategoryId) {
        return subcategoryRepository.findById(subcategoryId)
                .orElseThrow(() -> new BusinessException("CATALOG_SUBCATEGORY_NOT_FOUND",
                        "Subcategory not found", HttpStatus.NOT_FOUND));
    }

    private ProductResponseDto toDto(CatalogProduct p) {
        return ProductResponseDto.builder()
                .id(p.getId())
                .name(p.getName())
                .description(p.getDescription())
                .categoryId(p.getCategory().getId())
                .categoryName(p.getCategory().getName())
                .subcategoryId(p.getSubcategory().getId())
                .subcategoryName(p.getSubcategory().getName())
                .price(p.getPrice())
                .imageUrl(p.getImageUrl())
                .quantityUnit(p.getQuantityUnit())
                .quantityValue(p.getQuantityValue())
                .isActive(p.isActive())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }
}
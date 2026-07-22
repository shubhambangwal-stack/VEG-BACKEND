package com.veggofresh.vendor.service.impl;

import com.veggofresh.platform.exception.BusinessException;
import com.veggofresh.vendor.dto.ProductDto;
import com.veggofresh.vendor.dto.ShopDto;
import com.veggofresh.vendor.entity.Product;
import com.veggofresh.vendor.repository.ProductRepository;
import com.veggofresh.vendor.service.ProductCatalogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductCatalogServiceImpl implements ProductCatalogService {

    private final ProductRepository productRepository;

    private static final UUID MOCK_PRODUCT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID MOCK_SHOP_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Override
    @Transactional(readOnly = true)
    public ProductDto getProductById(UUID productId) {
        return productRepository.findByIdAndDeletedAtIsNull(productId)
                .filter(Product::isActive)
                .map(this::mapToDto)
                .orElseThrow(() -> new BusinessException("PRODUCT_NOT_FOUND", "Product not found or inactive"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductDto> getAvailableProducts(UUID shopId) {
        return productRepository.findAllByShopIdAndIsActiveTrueAndDeletedAtIsNull(shopId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }
    
    private ProductDto mapToDto(Product product) {
        return ProductDto.builder()
                .id(product.getId())
                .name(product.getName())
                .price(product.getPrice())
                .description(product.getDescription())
                .shopId(product.getShop().getId())
                .shopName(product.getShop().getName())
                .category(product.getCategory().getName())
                .build();
    }

    @Override
    public List<ShopDto> browseNearbyShops(double latitude, double longitude) {
        log.info("Stub browseNearbyShops called with lat: {}, lng: {}", latitude, longitude);
        ShopDto mockShop = ShopDto.builder()
                .id(MOCK_SHOP_ID)
                .name("VegGo Fresh Premium Shop")
                .latitude(latitude)
                .longitude(longitude)
                .distanceInKm(0.1)
                .build();
        return List.of(mockShop);
    }

    @Override
    public Page<ProductDto> searchProducts(String query, String category, Double minPrice, Double maxPrice, Pageable pageable) {
        log.info("Stub searchProducts called with query: {}, category: {}", query, category);
        ProductDto mockProduct = ProductDto.builder()
                .id(MOCK_PRODUCT_ID)
                .name("Fresh Organic Tomatoes")
                .price(BigDecimal.valueOf(49.99))
                .description("Farm fresh organic red tomatoes")
                .shopId(MOCK_SHOP_ID)
                .shopName("VegGo Fresh Premium Shop")
                .category("VEGETABLES")
                .build();
        return new PageImpl<>(List.of(mockProduct), pageable, 1);
    }
}

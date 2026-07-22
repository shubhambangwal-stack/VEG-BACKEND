package com.veggofresh.vendor.service.impl;

<<<<<<< HEAD
import com.veggofresh.platform.exception.BusinessException;
import com.veggofresh.vendor.dto.response.ProductDto;
import com.veggofresh.vendor.entity.InventoryItem;
import com.veggofresh.vendor.entity.Product;
import com.veggofresh.vendor.repository.InventoryItemRepository;
import com.veggofresh.vendor.repository.ProductRepository;
import com.veggofresh.vendor.service.ProductCatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductCatalogServiceImpl implements ProductCatalogService {

    private final ProductRepository productRepository;
    private final InventoryItemRepository inventoryItemRepository;

    @Override
    @Transactional(readOnly = true)
    public Optional<ProductDto> getProductById(UUID productId) {
        return productRepository.findByIdAndDeletedAtIsNull(productId)
                .filter(Product::isActive)
                .map(this::mapToDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductDto> getAvailableProducts(UUID shopId) {
        return productRepository.findAllByShopIdAndIsActiveTrueAndDeletedAtIsNull(shopId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }
    
    private ProductDto mapToDto(Product product) {
        InventoryItem inventory = inventoryItemRepository.findByProductIdAndDeletedAtIsNull(product.getId()).orElse(null);
        return ProductDto.builder()
                .id(product.getId())
                .shopId(product.getShop().getId())
                .categoryId(product.getCategory().getId())
                .categoryName(product.getCategory().getName())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .imageUrl(product.getImageUrl())
                .isActive(product.isActive())
                .stockQuantity(inventory != null ? inventory.getStockQuantity() : 0)
                .inStock(inventory != null && inventory.getStockQuantity() > 0)
=======
import com.veggofresh.vendor.dto.ProductDto;
import com.veggofresh.vendor.dto.ShopDto;
import com.veggofresh.vendor.service.ProductCatalogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * TODO: Vendor module implements this — do not remove, integration point.
 * This is a temporary stub returning mocked catalog data so that the customer module compiles and runs.
 */
@Slf4j
@Service
public class ProductCatalogServiceImpl implements ProductCatalogService {

    private static final UUID MOCK_PRODUCT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID MOCK_SHOP_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

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

    @Override
    public ProductDto getProductById(UUID productId) {
        log.info("Stub getProductById called with id: {}", productId);
        // For testing, return a default mock product for any UUID requested
        return ProductDto.builder()
                .id(productId)
                .name("Fresh Farm Spinach")
                .price(BigDecimal.valueOf(25.00))
                .description("Crispy spinach leaves")
                .shopId(MOCK_SHOP_ID)
                .shopName("VegGo Fresh Premium Shop")
                .category("GREEN_LEAFY")
>>>>>>> origin/main
                .build();
    }
}

package com.veggofresh.vendor.service.impl;

import com.veggofresh.platform.exception.BusinessException;
import com.veggofresh.vendor.dto.ProductDto;
import com.veggofresh.vendor.dto.ShopDto;
import com.veggofresh.vendor.entity.KycStatus;
import com.veggofresh.vendor.entity.Product;
import com.veggofresh.vendor.entity.Shop;
import com.veggofresh.vendor.repository.ProductRepository;
import com.veggofresh.vendor.repository.ShopRepository;
import com.veggofresh.vendor.service.ProductCatalogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductCatalogServiceImpl implements ProductCatalogService {

    private final ProductRepository productRepository;
    private final ShopRepository shopRepository;
    private final com.veggofresh.vendor.repository.CategoryRepository categoryRepository;

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

    @Override
    @Transactional(readOnly = true)
    public List<com.veggofresh.vendor.dto.CategoryDto> getAllCategories() {
        return categoryRepository.findAllByDeletedAtIsNull().stream()
                .map(category -> com.veggofresh.vendor.dto.CategoryDto.builder()
                        .id(category.getId())
                        .name(category.getName())
                        .description(category.getDescription())
                        .iconUrl(category.getIconUrl())
                        .isActive(category.isActive())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductDto> getRelatedProducts(UUID productId) {
        Product product = productRepository.findByIdAndDeletedAtIsNull(productId)
                .orElseThrow(() -> new BusinessException("PRODUCT_NOT_FOUND", "Product not found"));
        
        // Find other active products in the same category (excluding the current one)
        List<Product> products = productRepository.findAllByShopIdAndIsActiveTrueAndDeletedAtIsNull(product.getShop().getId());
        return products.stream()
                .filter(p -> p.getCategory().getId().equals(product.getCategory().getId()) && !p.getId().equals(productId))
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductDto> getDailyDeals() {
        // Return products that have a discount percent set or are marked as best seller
        List<Product> allProducts = productRepository.findAll();
        return allProducts.stream()
                .filter(p -> p.isActive() && p.getDeletedAt() == null && (p.getDiscountPercent() != null && p.getDiscountPercent() > 0))
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }
    
    private ProductDto mapToDto(Product product) {
        List<String> highlights = null;
        if (product.getWhyItsGreat() != null) {
            highlights = java.util.Arrays.stream(product.getWhyItsGreat().split(";"))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
        }

        return ProductDto.builder()
                .id(product.getId())
                .name(product.getName())
                .price(product.getPrice())
                .description(product.getDescription())
                .shopId(product.getShop().getId())
                .shopName(product.getShop().getName())
                .category(product.getCategory().getName())
                .imageUrl(product.getImageUrl())
                .unit(product.getUnit())
                .isBestSeller(product.isBestSeller())
                .discountPercent(product.getDiscountPercent())
                .badge(product.getBadge())
                .whyItsGreat(highlights)
                .storageTips(product.getStorageTips())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShopDto> browseNearbyShops(double latitude, double longitude) {
        log.info("browseNearbyShops called with lat: {}, lng: {}", latitude, longitude);
        List<Shop> shops = shopRepository.findAllByDeletedAtIsNullAndIsOnlineTrueAndKycStatus(KycStatus.APPROVED);
        
        return shops.stream()
                .filter(shop -> shop.getLatitude() != null && shop.getLongitude() != null)
                .map(shop -> {
                    double distance = calculateDistanceInKm(latitude, longitude, shop.getLatitude(), shop.getLongitude());
                    return ShopDto.builder()
                            .id(shop.getId())
                            .name(shop.getName())
                            .latitude(shop.getLatitude())
                            .longitude(shop.getLongitude())
                            .distanceInKm(distance)
                            .build();
                })
                .sorted(java.util.Comparator.comparingDouble(ShopDto::getDistanceInKm))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductDto> searchProducts(String query, String category, Double minPrice, Double maxPrice, Pageable pageable) {
        log.info("searchProducts called with query: {}, category: {}, minPrice: {}, maxPrice: {}", query, category, minPrice, maxPrice);
        
        String queryParam = (query != null && !query.trim().isEmpty()) ? query.trim() : null;
        String categoryParam = (category != null && !category.trim().isEmpty()) ? category.trim() : null;
        
        BigDecimal minPriceBd = minPrice != null ? BigDecimal.valueOf(minPrice) : null;
        BigDecimal maxPriceBd = maxPrice != null ? BigDecimal.valueOf(maxPrice) : null;
        
        Page<Product> products = productRepository.searchActiveProducts(queryParam, categoryParam, minPriceBd, maxPriceBd, pageable);
        
        return products.map(this::mapToDto);
    }

    private double calculateDistanceInKm(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Radius of the earth in km
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = R * c;
        return Math.round(distance * 10.0) / 10.0;
    }
}


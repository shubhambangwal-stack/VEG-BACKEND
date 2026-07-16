package com.veggofresh.vendor.service.impl;

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
                .build();
    }
}

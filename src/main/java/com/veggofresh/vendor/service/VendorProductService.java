package com.veggofresh.vendor.service;

import com.veggofresh.platform.exception.BusinessException;
import com.veggofresh.vendor.dto.request.ProductCreateRequestDto;
import com.veggofresh.vendor.dto.request.ProductUpdateRequestDto;
import com.veggofresh.vendor.dto.response.ProductDto;
import com.veggofresh.vendor.entity.Category;
import com.veggofresh.vendor.entity.InventoryItem;
import com.veggofresh.vendor.entity.Product;
import com.veggofresh.vendor.entity.Shop;
import com.veggofresh.vendor.repository.CategoryRepository;
import com.veggofresh.vendor.repository.InventoryItemRepository;
import com.veggofresh.vendor.repository.ProductRepository;
import com.veggofresh.vendor.repository.ShopRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VendorProductService {

    private final ProductRepository productRepository;
    private final ShopRepository shopRepository;
    private final CategoryRepository categoryRepository;
    private final InventoryItemRepository inventoryItemRepository;

    @Transactional
    public ProductDto addProduct(UUID ownerUserId, ProductCreateRequestDto request) {
        Shop shop = shopRepository.findByIdAndDeletedAtIsNull(request.getShopId())
                .orElseThrow(() -> new BusinessException("Shop not found", "VENDOR_SHOP_NOT_FOUND"));

        if (!shop.getOwnerUserId().equals(ownerUserId)) {
            throw new BusinessException("Unauthorized to add product to this shop", "VENDOR_UNAUTHORIZED");
        }

        Category category = categoryRepository.findByIdAndDeletedAtIsNull(request.getCategoryId())
                .orElseThrow(() -> new BusinessException("Category not found", "VENDOR_CATEGORY_NOT_FOUND"));

        Product product = Product.builder()
                .shop(shop)
                .category(category)
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .imageUrl(request.getImageUrl())
                .isActive(true)
                .build();

        product = productRepository.save(product);

        // Auto-create inventory for the product
        InventoryItem inventoryItem = InventoryItem.builder()
                .product(product)
                .stockQuantity(0)
                .lowStockThreshold(0)
                .build();
        inventoryItemRepository.save(inventoryItem);

        return mapToDto(product, inventoryItem);
    }

    @Transactional
    public ProductDto updateProduct(UUID ownerUserId, UUID productId, ProductUpdateRequestDto request) {
        Product product = productRepository.findByIdAndDeletedAtIsNull(productId)
                .orElseThrow(() -> new BusinessException("Product not found", "VENDOR_PRODUCT_NOT_FOUND"));

        if (!product.getShop().getOwnerUserId().equals(ownerUserId)) {
            throw new BusinessException("Unauthorized to modify this product", "VENDOR_UNAUTHORIZED");
        }

        Category category = categoryRepository.findByIdAndDeletedAtIsNull(request.getCategoryId())
                .orElseThrow(() -> new BusinessException("Category not found", "VENDOR_CATEGORY_NOT_FOUND"));

        product.setCategory(category);
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        
        if (request.getIsActive() != null) {
            product.setActive(request.getIsActive());
        }

        product = productRepository.save(product);
        InventoryItem inventory = inventoryItemRepository.findByProductIdAndDeletedAtIsNull(product.getId()).orElse(null);
        return mapToDto(product, inventory);
    }

    @Transactional
    public void deleteProduct(UUID ownerUserId, UUID productId) {
        Product product = productRepository.findByIdAndDeletedAtIsNull(productId)
                .orElseThrow(() -> new BusinessException("Product not found", "VENDOR_PRODUCT_NOT_FOUND"));

        if (!product.getShop().getOwnerUserId().equals(ownerUserId)) {
            throw new BusinessException("Unauthorized to delete this product", "VENDOR_UNAUTHORIZED");
        }

        product.softDelete();
        productRepository.save(product);
        
        inventoryItemRepository.findByProductIdAndDeletedAtIsNull(productId)
            .ifPresent(inventory -> {
                inventory.softDelete();
                inventoryItemRepository.save(inventory);
            });
    }

    @Transactional(readOnly = true)
    public List<ProductDto> getProductsByShop(UUID ownerUserId) {
        Shop shop = shopRepository.findByOwnerUserIdAndDeletedAtIsNull(ownerUserId)
                .orElseThrow(() -> new BusinessException("Shop not found", "VENDOR_SHOP_NOT_FOUND"));

        return productRepository.findAllByShopIdAndDeletedAtIsNull(shop.getId()).stream()
                .map(product -> {
                    InventoryItem inv = inventoryItemRepository.findByProductIdAndDeletedAtIsNull(product.getId()).orElse(null);
                    return mapToDto(product, inv);
                })
                .collect(Collectors.toList());
    }

    public ProductDto mapToDto(Product product, InventoryItem inventory) {
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

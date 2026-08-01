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
                .orElseThrow(() -> new BusinessException("VENDOR_SHOP_NOT_FOUND", "Shop not found"));

        if (!shop.getOwnerUserId().equals(ownerUserId)) {
            throw new BusinessException("VENDOR_UNAUTHORIZED", "Unauthorized to add product to this shop");
        }

        Category category = categoryRepository.findByIdAndDeletedAtIsNull(request.getCategoryId())
                .orElseThrow(() -> new BusinessException("VENDOR_CATEGORY_NOT_FOUND", "Category not found"));

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
                .orElseThrow(() -> new BusinessException("VENDOR_PRODUCT_NOT_FOUND", "Product not found"));

        if (!product.getShop().getOwnerUserId().equals(ownerUserId)) {
            throw new BusinessException("VENDOR_UNAUTHORIZED", "Unauthorized to modify this product");
        }

        Category category = categoryRepository.findByIdAndDeletedAtIsNull(request.getCategoryId())
                .orElseThrow(() -> new BusinessException("VENDOR_CATEGORY_NOT_FOUND", "Category not found"));

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
    public ProductDto updateProductImage(UUID ownerUserId, UUID productId, String imageUrl) {
        Product product = productRepository.findByIdAndDeletedAtIsNull(productId)
                .orElseThrow(() -> new BusinessException("VENDOR_PRODUCT_NOT_FOUND", "Product not found"));

        if (!product.getShop().getOwnerUserId().equals(ownerUserId)) {
            throw new BusinessException("VENDOR_UNAUTHORIZED", "Unauthorized to modify this product");
        }

        product.setImageUrl(imageUrl);
        product = productRepository.save(product);
        InventoryItem inventory = inventoryItemRepository.findByProductIdAndDeletedAtIsNull(product.getId()).orElse(null);
        return mapToDto(product, inventory);
    }

    @Transactional
    public void deleteProduct(UUID ownerUserId, UUID productId) {
        Product product = productRepository.findByIdAndDeletedAtIsNull(productId)
                .orElseThrow(() -> new BusinessException("VENDOR_PRODUCT_NOT_FOUND", "Product not found"));

        if (!product.getShop().getOwnerUserId().equals(ownerUserId)) {
            throw new BusinessException("VENDOR_UNAUTHORIZED", "Unauthorized to delete this product");
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
                .orElseThrow(() -> new BusinessException("VENDOR_SHOP_NOT_FOUND", "Shop not found"));

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

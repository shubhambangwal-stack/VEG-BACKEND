package com.veggofresh.vendor.service;

import com.veggofresh.platform.exception.BusinessException;
import com.veggofresh.vendor.dto.request.InventoryUpdateRequestDto;
import com.veggofresh.vendor.dto.response.InventoryItemDto;
import com.veggofresh.vendor.entity.InventoryItem;
import com.veggofresh.vendor.entity.Product;
import com.veggofresh.vendor.repository.InventoryItemRepository;
import com.veggofresh.vendor.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VendorInventoryService {

    private final InventoryItemRepository inventoryItemRepository;
    private final ProductRepository productRepository;

    @Transactional
    public InventoryItemDto updateStock(UUID ownerUserId, UUID productId, InventoryUpdateRequestDto request) {
        Product product = productRepository.findByIdAndDeletedAtIsNull(productId)
                .orElseThrow(() -> new BusinessException("Product not found", "VENDOR_PRODUCT_NOT_FOUND"));

        if (!product.getShop().getOwnerUserId().equals(ownerUserId)) {
            throw new BusinessException("Unauthorized to modify this inventory", "VENDOR_UNAUTHORIZED");
        }

        InventoryItem inventory = inventoryItemRepository.findByProductIdAndDeletedAtIsNull(productId)
                .orElseThrow(() -> new BusinessException("Inventory record not found", "VENDOR_INVENTORY_NOT_FOUND"));

        inventory.setStockQuantity(request.getStockQuantity());
        if (request.getLowStockThreshold() != null) {
            inventory.setLowStockThreshold(request.getLowStockThreshold());
        }

        inventory = inventoryItemRepository.save(inventory);
        return mapToDto(inventory);
    }
    
    @Transactional
    public void deductStock(UUID productId, int quantity) {
        InventoryItem inventory = inventoryItemRepository.findByProductIdAndDeletedAtIsNull(productId)
                .orElseThrow(() -> new BusinessException("Inventory record not found", "VENDOR_INVENTORY_NOT_FOUND"));
                
        if (inventory.getStockQuantity() < quantity) {
            throw new BusinessException("Insufficient stock", "VENDOR_INSUFFICIENT_STOCK");
        }
        
        inventory.setStockQuantity(inventory.getStockQuantity() - quantity);
        inventoryItemRepository.save(inventory);
    }

    private InventoryItemDto mapToDto(InventoryItem inventory) {
        return InventoryItemDto.builder()
                .id(inventory.getId())
                .productId(inventory.getProduct().getId())
                .stockQuantity(inventory.getStockQuantity())
                .lowStockThreshold(inventory.getLowStockThreshold())
                .build();
    }
}

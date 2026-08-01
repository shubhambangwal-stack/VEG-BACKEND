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
                .orElseThrow(() -> new BusinessException("VENDOR_PRODUCT_NOT_FOUND", "Product not found"));

        if (!product.getShop().getOwnerUserId().equals(ownerUserId)) {
            throw new BusinessException("VENDOR_UNAUTHORIZED", "Unauthorized to modify this inventory");
        }

        InventoryItem inventory = inventoryItemRepository.findByProductIdAndDeletedAtIsNull(productId)
                .orElseThrow(() -> new BusinessException("VENDOR_INVENTORY_NOT_FOUND", "Inventory record not found"));

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
                .orElseThrow(() -> new BusinessException("VENDOR_INVENTORY_NOT_FOUND", "Inventory record not found"));
                
        if (inventory.getStockQuantity() < quantity) {
            throw new BusinessException("VENDOR_INSUFFICIENT_STOCK", "Insufficient stock");
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

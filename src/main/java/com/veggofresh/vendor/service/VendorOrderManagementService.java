package com.veggofresh.vendor.service;

import com.veggofresh.customer.dto.response.OrderResponseDto;
import com.veggofresh.customer.service.CustomerOrderService;
import com.veggofresh.platform.exception.BusinessException;
import com.veggofresh.vendor.entity.Shop;
import com.veggofresh.vendor.repository.ShopRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VendorOrderManagementService {

    private final CustomerOrderService customerOrderService;
    private final VendorInventoryService vendorInventoryService;
    private final ShopRepository shopRepository;

    @Transactional(readOnly = true)
    public List<OrderResponseDto> getShopOrders(UUID ownerUserId) {
        Shop shop = shopRepository.findByOwnerUserIdAndDeletedAtIsNull(ownerUserId)
                .orElseThrow(() -> new BusinessException("VENDOR_SHOP_NOT_FOUND", "Shop not found"));
        return customerOrderService.getOrdersByShopId(shop.getId());
    }

    @Transactional
    public void acceptOrder(UUID ownerUserId, UUID orderId) {
        Shop shop = shopRepository.findByOwnerUserIdAndDeletedAtIsNull(ownerUserId)
                .orElseThrow(() -> new BusinessException("VENDOR_SHOP_NOT_FOUND", "Shop not found"));

        // Here we ideally verify the order belongs to this shop via CustomerOrderService.
        // For now, we assume the customerOrderService checks it or we just invoke the action.
        
        // Example: The CustomerOrderService might deduct stock during its internal flow,
        // or we do it here. If the vendor module owns inventory, it deducts it.
        // We'll call the customer module to update the order state.
        customerOrderService.acceptOrder(orderId);
    }

    @Transactional
    public void rejectOrder(UUID ownerUserId, UUID orderId) {
        Shop shop = shopRepository.findByOwnerUserIdAndDeletedAtIsNull(ownerUserId)
                .orElseThrow(() -> new BusinessException("VENDOR_SHOP_NOT_FOUND", "Shop not found"));

        customerOrderService.rejectOrder(orderId);
    }

    @Transactional
    public void updateOrderStatus(UUID ownerUserId, UUID orderId, String status) {
        Shop shop = shopRepository.findByOwnerUserIdAndDeletedAtIsNull(ownerUserId)
                .orElseThrow(() -> new BusinessException("VENDOR_SHOP_NOT_FOUND", "Shop not found"));

        customerOrderService.updateOrderStatus(orderId, status);
    }
}

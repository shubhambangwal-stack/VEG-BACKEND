package com.veggofresh.vendor.service;

import com.veggofresh.platform.exception.BusinessException;
import com.veggofresh.vendor.dto.response.DashboardSummaryResponseDto;
import com.veggofresh.vendor.entity.InventoryItem;
import com.veggofresh.vendor.entity.Product;
import com.veggofresh.vendor.entity.Shop;
import com.veggofresh.vendor.repository.InventoryItemRepository;
import com.veggofresh.vendor.repository.ProductRepository;
import com.veggofresh.vendor.repository.ShopRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VendorDashboardService {

    private final ShopRepository shopRepository;
    private final ProductRepository productRepository;
    private final InventoryItemRepository inventoryItemRepository;

    @Transactional(readOnly = true)
    public DashboardSummaryResponseDto getDashboardSummary(UUID ownerUserId) {
        Shop shop = shopRepository.findByOwnerUserIdAndDeletedAtIsNull(ownerUserId)
                .orElseThrow(() -> new BusinessException("SHOP_NOT_FOUND", "Vendor shop not found"));

        List<Product> products = productRepository.findAllByShopIdAndDeletedAtIsNull(shop.getId());
        long outOfStockCount = products.stream()
                .map(product -> inventoryItemRepository.findByProductIdAndDeletedAtIsNull(product.getId()))
                .filter(opt -> opt.isPresent() && opt.get().getStockQuantity() <= 0)
                .count();

        return DashboardSummaryResponseDto.builder()
                .businessName(shop.getName())
                .todaysRevenue(0.0)
                .revenueChangePercent(0.0)
                .activeOrdersCount(0)
                .pendingPickupCount(0)
                .outOfStockCount((int) outOfStockCount)
                .performanceTrend(List.of(0, 0, 0, 0, 0, 0, 0))
                .recentOrders(Collections.emptyList())
                .build();
    }
}

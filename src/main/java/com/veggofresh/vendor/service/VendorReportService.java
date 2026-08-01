package com.veggofresh.vendor.service;

import com.veggofresh.customer.entity.Order;
import com.veggofresh.customer.entity.OrderStatus;
import com.veggofresh.customer.repository.OrderRepository;
import com.veggofresh.platform.exception.BusinessException;
import com.veggofresh.vendor.entity.Product;
import com.veggofresh.vendor.entity.Shop;
import com.veggofresh.vendor.repository.ProductRepository;
import com.veggofresh.vendor.repository.ShopRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VendorReportService {

    private final ShopRepository shopRepository;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public Map<String, Object> getSalesReports(UUID ownerUserId) {
        Shop shop = shopRepository.findByOwnerUserIdAndDeletedAtIsNull(ownerUserId)
                .orElseThrow(() -> new BusinessException("VENDOR_SHOP_NOT_FOUND", "Shop not found"));
        
        List<Order> orders = orderRepository.findByShopId(shop.getId());
        
        long totalSalesCount = orders.stream()
                .filter(o -> o.getStatus() == OrderStatus.DELIVERED)
                .count();

        BigDecimal totalRevenue = orders.stream()
                .filter(o -> o.getStatus() == OrderStatus.DELIVERED)
                .map(o -> calculateShopRevenueForOrder(o, shop.getId()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Integer> salesByProduct = new HashMap<>();
        orders.stream()
                .filter(o -> o.getStatus() == OrderStatus.DELIVERED)
                .flatMap(o -> o.getItems().stream())
                .forEach(item -> {
                    Product product = productRepository.findByIdAndDeletedAtIsNull(item.getProductId()).orElse(null);
                    if (product != null && product.getShop().getId().equals(shop.getId())) {
                        salesByProduct.merge(product.getName(), item.getQuantity(), Integer::sum);
                    }
                });

        return Map.of(
                "shopId", shop.getId(),
                "totalSalesCount", totalSalesCount,
                "totalRevenue", totalRevenue.doubleValue(),
                "salesByProduct", salesByProduct
        );
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getEarningsReports(UUID ownerUserId) {
        Shop shop = shopRepository.findByOwnerUserIdAndDeletedAtIsNull(ownerUserId)
                .orElseThrow(() -> new BusinessException("VENDOR_SHOP_NOT_FOUND", "Shop not found"));
        
        List<Order> orders = orderRepository.findByShopId(shop.getId());

        BigDecimal totalEarnings = orders.stream()
                .filter(o -> o.getStatus() == OrderStatus.DELIVERED)
                .map(o -> calculateShopRevenueForOrder(o, shop.getId()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal pendingEarnings = orders.stream()
                .filter(o -> o.getStatus() != OrderStatus.DELIVERED && o.getStatus() != OrderStatus.CANCELLED)
                .map(o -> calculateShopRevenueForOrder(o, shop.getId()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return Map.of(
                "shopId", shop.getId(),
                "totalEarnings", totalEarnings.doubleValue(),
                "pendingEarnings", pendingEarnings.doubleValue(),
                "currency", "INR"
        );
    }

    private BigDecimal calculateShopRevenueForOrder(Order order, UUID shopId) {
        return order.getItems().stream()
                .map(item -> {
                    Product product = productRepository.findByIdAndDeletedAtIsNull(item.getProductId()).orElse(null);
                    if (product != null && product.getShop().getId().equals(shopId)) {
                        return item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
                    }
                    return BigDecimal.ZERO;
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}

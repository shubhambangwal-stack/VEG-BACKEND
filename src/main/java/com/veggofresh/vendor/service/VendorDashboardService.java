package com.veggofresh.vendor.service;

import com.veggofresh.customer.entity.Order;
import com.veggofresh.customer.entity.OrderStatus;
import com.veggofresh.customer.repository.OrderRepository;
import com.veggofresh.platform.exception.BusinessException;
import com.veggofresh.vendor.dto.response.DashboardSummaryResponseDto;
import com.veggofresh.vendor.dto.response.RecentOrderDto;
import com.veggofresh.vendor.entity.Product;
import com.veggofresh.vendor.entity.Shop;
import com.veggofresh.vendor.repository.InventoryItemRepository;
import com.veggofresh.vendor.repository.ProductRepository;
import com.veggofresh.vendor.repository.ShopRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VendorDashboardService {

    private final ShopRepository shopRepository;
    private final ProductRepository productRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final OrderRepository orderRepository;

    @Transactional(readOnly = true)
    public DashboardSummaryResponseDto getDashboardSummary(UUID ownerUserId) {
        Shop shop = shopRepository.findByOwnerUserIdAndDeletedAtIsNull(ownerUserId)
                .orElseThrow(() -> new BusinessException("SHOP_NOT_FOUND", "Vendor shop not found"));

        List<Product> products = productRepository.findAllByShopIdAndDeletedAtIsNull(shop.getId());
        long outOfStockCount = products.stream()
                .map(product -> inventoryItemRepository.findByProductIdAndDeletedAtIsNull(product.getId()))
                .filter(opt -> opt.isPresent() && opt.get().getStockQuantity() <= 0)
                .count();

        List<Order> allShopOrders = orderRepository.findByShopId(shop.getId());

        Instant startOfToday = Instant.now().truncatedTo(ChronoUnit.DAYS);

        // 1. Today's Revenue
        BigDecimal todaysRevenue = allShopOrders.stream()
                .filter(order -> order.getCreatedAt().isAfter(startOfToday))
                .map(order -> calculateShopRevenueForOrder(order, shop.getId()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 2. Revenue Change Percent (vs Yesterday)
        Instant startOfYesterday = startOfToday.minus(1, ChronoUnit.DAYS);
        BigDecimal yesterdaysRevenue = allShopOrders.stream()
                .filter(order -> order.getCreatedAt().isAfter(startOfYesterday) && order.getCreatedAt().isBefore(startOfToday))
                .map(order -> calculateShopRevenueForOrder(order, shop.getId()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        double changePercent = 0.0;
        if (yesterdaysRevenue.compareTo(BigDecimal.ZERO) > 0) {
            changePercent = todaysRevenue.subtract(yesterdaysRevenue)
                    .divide(yesterdaysRevenue, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .doubleValue();
        }

        // 3. Active Orders Count
        long activeOrdersCount = allShopOrders.stream()
                .filter(order -> order.getStatus() != OrderStatus.DELIVERED && order.getStatus() != OrderStatus.CANCELLED)
                .count();

        // 4. Pending Pickup Count (Confirmed/Preparing)
        long pendingPickupCount = allShopOrders.stream()
                .filter(order -> order.getStatus() == OrderStatus.CONFIRMED || order.getStatus() == OrderStatus.PREPARING)
                .count();

        // 5. Performance Trend (Last 7 Days)
        List<Integer> performanceTrend = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            Instant dayStart = startOfToday.minus(i, ChronoUnit.DAYS);
            Instant dayEnd = dayStart.plus(1, ChronoUnit.DAYS);
            BigDecimal dayRevenue = allShopOrders.stream()
                    .filter(order -> order.getCreatedAt().isAfter(dayStart) && order.getCreatedAt().isBefore(dayEnd))
                    .map(order -> calculateShopRevenueForOrder(order, shop.getId()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            performanceTrend.add(dayRevenue.intValue());
        }

        // 6. Recent Orders (Top 5)
        List<RecentOrderDto> recentOrders = allShopOrders.stream()
                .sorted((o1, o2) -> o2.getCreatedAt().compareTo(o1.getCreatedAt()))
                .limit(5)
                .map(order -> mapToRecentOrderDto(order, shop.getId()))
                .collect(Collectors.toList());

        return DashboardSummaryResponseDto.builder()
                .businessName(shop.getName())
                .todaysRevenue(todaysRevenue.doubleValue())
                .revenueChangePercent(changePercent)
                .activeOrdersCount((int) activeOrdersCount)
                .pendingPickupCount((int) pendingPickupCount)
                .outOfStockCount((int) outOfStockCount)
                .performanceTrend(performanceTrend)
                .recentOrders(recentOrders)
                .build();
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

    private RecentOrderDto mapToRecentOrderDto(Order order, UUID shopId) {
        String itemsSummary = order.getItems().stream()
                .map(item -> {
                    Product product = productRepository.findByIdAndDeletedAtIsNull(item.getProductId()).orElse(null);
                    String name = product != null ? product.getName() : "Unknown Product";
                    return name + " x " + item.getQuantity();
                })
                .collect(Collectors.joining(", "));

        BigDecimal shopAmount = calculateShopRevenueForOrder(order, shopId);

        return RecentOrderDto.builder()
                .orderNumber(order.getId().toString().substring(0, 8).toUpperCase())
                .itemsSummary(itemsSummary)
                .timeAgo(formatTimeAgo(order.getCreatedAt()))
                .amount(shopAmount.doubleValue())
                .status(order.getStatus().name().toLowerCase())
                .build();
    }

    private String formatTimeAgo(Instant createdAt) {
        long minutes = ChronoUnit.MINUTES.between(createdAt, Instant.now());
        if (minutes < 1) return "Just now";
        if (minutes < 60) return minutes + " mins ago";
        long hours = ChronoUnit.HOURS.between(createdAt, Instant.now());
        if (hours < 24) return hours + " hours ago";
        long days = ChronoUnit.DAYS.between(createdAt, Instant.now());
        return days + " days ago";
    }
}

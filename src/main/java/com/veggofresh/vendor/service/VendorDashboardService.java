package com.veggofresh.vendor.service;

import com.veggofresh.admin.service.AdminProductService;
import com.veggofresh.customer.entity.Order;
import com.veggofresh.customer.entity.OrderStatus;
import com.veggofresh.customer.repository.OrderRepository;
import com.veggofresh.platform.exception.BusinessException;
import com.veggofresh.vendor.dto.response.DashboardSummaryResponseDto;
import com.veggofresh.vendor.dto.response.RecentOrderDto;
import com.veggofresh.vendor.entity.Shop;
import com.veggofresh.vendor.repository.ShopRepository;
import com.veggofresh.vendor.repository.VendorListingRepository;
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

/**
 * NOT YET FIXED: still directly imports Customer's Order/OrderStatus entities and
 * injects Customer's OrderRepository directly instead of going through
 * CustomerOrderService -- flagged as a boundary violation in the Vendor audit,
 * deliberately deferred past the onboarding phase. See NOTES_VENDOR.md.
 */
@Service
@RequiredArgsConstructor
public class VendorDashboardService {

    private final ShopRepository shopRepository;
    private final VendorListingRepository vendorListingRepository;
    private final AdminProductService adminProductService;
    private final OrderRepository orderRepository;

    @Transactional(readOnly = true)
    public DashboardSummaryResponseDto getDashboardSummary(UUID ownerUserId) {
        Shop shop = shopRepository.findByOwnerUserIdAndDeletedAtIsNull(ownerUserId)
                .orElseThrow(() -> new BusinessException("SHOP_NOT_FOUND", "Vendor shop not found"));

        // NEW ARCHITECTURE: stock is no longer tracked at all (vendor accepts/
        // rejects each order manually based on real stock at the time) -- this
        // card is vestigial, kept at 0 rather than removed from the DTO/API
        // contract outright. See NOTES_VENDOR.md.
        long outOfStockCount = 0;

        List<Order> allShopOrders = orderRepository.findByAcceptedShopId(shop.getId());

        Instant startOfToday = Instant.now().truncatedTo(ChronoUnit.DAYS);

        BigDecimal todaysRevenue = allShopOrders.stream()
                .filter(order -> order.getCreatedAt().isAfter(startOfToday))
                .map(order -> calculateShopRevenueForOrder(order, shop.getId()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

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

        long activeOrdersCount = allShopOrders.stream()
                .filter(order -> order.getStatus() != OrderStatus.DELIVERED && order.getStatus() != OrderStatus.CANCELLED)
                .count();

        long pendingPickupCount = allShopOrders.stream()
                .filter(order -> order.getStatus() == OrderStatus.CONFIRMED || order.getStatus() == OrderStatus.PREPARING)
                .count();

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
                    if (vendorListingRepository.findByShopIdAndCatalogProductId(shopId, item.getProductId()).isPresent()) {
                        return item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
                    }
                    return BigDecimal.ZERO;
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private RecentOrderDto mapToRecentOrderDto(Order order, UUID shopId) {
        String itemsSummary = order.getItems().stream()
                .map(item -> {
                    String name = resolveProductName(item.getProductId());
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

    private String resolveProductName(UUID catalogProductId) {
        try {
            return adminProductService.getProductById(catalogProductId).getName();
        } catch (Exception e) {
            return "Unknown Product";
        }
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

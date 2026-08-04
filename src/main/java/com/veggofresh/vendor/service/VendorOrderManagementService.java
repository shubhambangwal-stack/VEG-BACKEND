package com.veggofresh.vendor.service;

import com.veggofresh.auth.dto.UserSummaryDto;
import com.veggofresh.auth.service.UserLookupService;
import com.veggofresh.customer.dto.response.OrderItemResponseDto;
import com.veggofresh.customer.dto.response.OrderResponseDto;
import com.veggofresh.customer.service.CustomerOrderService;
import com.veggofresh.platform.exception.BusinessException;
import com.veggofresh.vendor.dto.response.VendorOrderDetailResponseDto;
import com.veggofresh.vendor.dto.response.VendorOrderItemDto;
import com.veggofresh.vendor.entity.Shop;
import com.veggofresh.vendor.repository.ProductRepository;
import com.veggofresh.vendor.repository.ShopRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VendorOrderManagementService {

    // Flat placeholder rate -- no real fee engine exists anywhere in the codebase.
    // Whoever builds real fee logic later should replace this constant.
    private static final BigDecimal SERVICE_FEE_PERCENT = BigDecimal.valueOf(10);

    private final CustomerOrderService customerOrderService;
    private final VendorInventoryService vendorInventoryService;
    private final ShopRepository shopRepository;
    private final ProductRepository productRepository;
    private final UserLookupService userLookupService;

    @Transactional(readOnly = true)
    public List<OrderResponseDto> getShopOrders(UUID ownerUserId) {
        Shop shop = requireShop(ownerUserId);
        return customerOrderService.getOrdersByShopId(shop.getId());
    }

    /**
     * Enriched single-order view for the Figma "Order Details" screen. Items are
     * FILTERED to only this shop's own products -- an order can span multiple
     * vendors, so subtotal/fee/total below are scoped accordingly, not the full
     * order's totalAmount. customerPhone resolved live via UserLookupService
     * (no phone denormalization needed -- always fresh).
     */
    @Transactional(readOnly = true)
    public VendorOrderDetailResponseDto getOrderDetail(UUID ownerUserId, UUID orderId) {
        Shop shop = requireShop(ownerUserId);

        OrderResponseDto order = customerOrderService.getOrdersByShopId(shop.getId()).stream()
                .filter(o -> o.getId().equals(orderId))
                .findFirst()
                .orElseThrow(() -> new BusinessException("VENDOR_ORDER_NOT_FOUND", "Order not found for this shop", HttpStatus.NOT_FOUND));

        List<VendorOrderItemDto> shopItems = order.getItems().stream()
                .filter(item -> belongsToShop(item, shop.getId()))
                .map(this::mapItemToDto)
                .collect(Collectors.toList());

        BigDecimal subtotal = shopItems.stream()
                .map(VendorOrderItemDto::getSubTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal serviceFee = subtotal.multiply(SERVICE_FEE_PERCENT)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        String customerPhone = userLookupService.findById(order.getUserId())
                .map(UserSummaryDto::getPhone)
                .orElse(null);

        return VendorOrderDetailResponseDto.builder()
                .orderId(order.getId())
                .status(order.getStatus())
                .items(shopItems)
                .customerPhone(customerPhone)
                .deliveryAddress(order.getDeliveryAddress())
                .latitude(order.getLatitude())
                .longitude(order.getLongitude())
                .subtotal(subtotal)
                .serviceFeePercent(SERVICE_FEE_PERCENT)
                .serviceFee(serviceFee)
                .totalForThisShop(subtotal.add(serviceFee))
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }

    @Transactional
    public void acceptOrder(UUID ownerUserId, UUID orderId) {
        requireShop(ownerUserId);
        customerOrderService.acceptOrder(orderId);
    }

    @Transactional
    public void rejectOrder(UUID ownerUserId, UUID orderId) {
        requireShop(ownerUserId);
        customerOrderService.rejectOrder(orderId);
    }

    @Transactional
    public void updateOrderStatus(UUID ownerUserId, UUID orderId, String status) {
        requireShop(ownerUserId);
        customerOrderService.updateOrderStatus(orderId, status);
    }

    private Shop requireShop(UUID ownerUserId) {
        return shopRepository.findByOwnerUserIdAndDeletedAtIsNull(ownerUserId)
                .orElseThrow(() -> new BusinessException("VENDOR_SHOP_NOT_FOUND", "Shop not found", HttpStatus.NOT_FOUND));
    }

    private boolean belongsToShop(OrderItemResponseDto item, UUID shopId) {
        return productRepository.findByIdAndDeletedAtIsNull(item.getProductId())
                .map(product -> product.getShop().getId().equals(shopId))
                .orElse(false);
    }

    private VendorOrderItemDto mapItemToDto(OrderItemResponseDto item) {
        return VendorOrderItemDto.builder()
                .productId(item.getProductId())
                .productName(item.getProductName())
                .quantity(item.getQuantity())
                .price(item.getPrice())
                .subTotal(item.getSubTotal())
                .build();
    }
}

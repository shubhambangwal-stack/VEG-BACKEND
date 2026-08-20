package com.veggofresh.vendor.service;

import com.veggofresh.auth.dto.UserSummaryDto;
import com.veggofresh.auth.service.UserLookupService;
import com.veggofresh.customer.dto.response.OrderItemResponseDto;
import com.veggofresh.customer.dto.response.OrderResponseDto;
import com.veggofresh.customer.service.CustomerOrderService;
import com.veggofresh.delivery.dto.VendorDeliveryStatusDto;
import com.veggofresh.delivery.service.DeliveryDispatchService;
import com.veggofresh.delivery.service.DeliveryPickupInfoService;
import com.veggofresh.platform.exception.BusinessException;
import com.veggofresh.vendor.dto.response.VendorOrderDetailResponseDto;
import com.veggofresh.vendor.dto.response.VendorOrderItemDto;
import com.veggofresh.vendor.entity.Shop;
import com.veggofresh.vendor.repository.ShopRepository;
import com.veggofresh.vendor.repository.VendorListingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * EXTENDED THIS ROUND -- the "mark ready for pickup" trigger (the whole point of the
 * Vendor round, per PROJECT_STATE sections 3/4), plus pickup-OTP and delivery-status
 * visibility. Full detail in NOTES_VENDOR.md.
 */
@Service
@RequiredArgsConstructor
public class VendorOrderManagementService {

    // Flat placeholder rate -- no real fee engine exists anywhere in the codebase.
    // Whoever builds real fee logic later should replace this constant.
    private static final BigDecimal SERVICE_FEE_PERCENT = BigDecimal.valueOf(10);

    private final CustomerOrderService customerOrderService;
    private final ShopRepository shopRepository;
    private final VendorListingRepository vendorListingRepository;
    private final UserLookupService userLookupService;
    private final DeliveryDispatchService deliveryDispatchService;
    private final DeliveryPickupInfoService deliveryPickupInfoService;

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
        OrderResponseDto order = requireShopOrder(shop, orderId);

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

    /**
     * NEW THIS ROUND -- the real dispatch trigger. Previously nothing in Vendor called
     * DeliveryDispatchService at all (only DeliveryTestController's /test/dispatch
     * exercised it). This is the vendor physically finishing prep and handing off to
     * delivery: flips the order to READY_FOR_PICKUP (new OrderStatus value -- see
     * Customer's OrderStatus.java), then dispatches to Delivery, which broadcasts to
     * eligible partners within Admin's configured radius of THIS shop's location and
     * issues the pickup OTP once someone accepts (see Delivery's NOTES_DELIVERY.md).
     */
    @Transactional
    public String markReadyForPickup(UUID ownerUserId, UUID orderId) {
        Shop shop = requireShop(ownerUserId);
        OrderResponseDto order = requireShopOrder(shop, orderId);

        if (shop.getLatitude() == null || shop.getLongitude() == null) {
            throw new BusinessException("VENDOR_SHOP_LOCATION_MISSING",
                    "Your shop's location isn't set -- update your shop profile before marking orders ready for pickup", HttpStatus.BAD_REQUEST);
        }

        // updateOrderStatus -> CustomerOrderService -> OrderService.updateOrderStatus
        // already validates this transition via OrderStatus.isValidTransition -- throws
        // INVALID_ORDER_STATE_TRANSITION on its own if the order isn't in a state this
        // is legal from (CONFIRMED or PREPARING).
        customerOrderService.updateOrderStatus(orderId, "READY_FOR_PICKUP");

        deliveryDispatchService.dispatchOrder(orderId, order.getUserId(), ownerUserId, shop.getName(), shop.getAddress(),
                shop.getLatitude(), shop.getLongitude(), order.getLatitude(), order.getLongitude());

        return "Order marked ready for pickup -- nearby delivery partners are being notified";
    }

    /**
     * NEW THIS ROUND -- shows the vendor the pickup OTP they need to hand over once a
     * delivery partner has accepted. Returns null (not an error) if nobody's accepted
     * yet -- there's nothing to show, that's an expected state, not a failure.
     */
    @Transactional(readOnly = true)
    public String getPickupOtp(UUID ownerUserId, UUID orderId) {
        requireShop(ownerUserId);
        return deliveryPickupInfoService.getPickupOtpForVendor(orderId, ownerUserId);
    }

    /**
     * NEW THIS ROUND -- "where's my delivery" for the vendor's own order-detail screen,
     * closing the gap where only Customer had any delivery-status visibility at all.
     */
    @Transactional(readOnly = true)
    public VendorDeliveryStatusDto getDeliveryStatus(UUID ownerUserId, UUID orderId) {
        requireShop(ownerUserId);
        return deliveryPickupInfoService.getDeliveryStatusForVendor(orderId, ownerUserId);
    }

    private Shop requireShop(UUID ownerUserId) {
        return shopRepository.findByOwnerUserIdAndDeletedAtIsNull(ownerUserId)
                .orElseThrow(() -> new BusinessException("VENDOR_SHOP_NOT_FOUND", "Shop not found", HttpStatus.NOT_FOUND));
    }

    private OrderResponseDto requireShopOrder(Shop shop, UUID orderId) {
        return customerOrderService.getOrdersByShopId(shop.getId()).stream()
                .filter(o -> o.getId().equals(orderId))
                .findFirst()
                .orElseThrow(() -> new BusinessException("VENDOR_ORDER_NOT_FOUND", "Order not found for this shop", HttpStatus.NOT_FOUND));
    }

    // NEW ARCHITECTURE: item.getProductId() is now a catalog product id --
    // "belongs to shop" means this shop currently has (or had) it listed,
    // not that the shop owns a legacy Product row.
    private boolean belongsToShop(OrderItemResponseDto item, UUID shopId) {
        return vendorListingRepository.findByShopIdAndCatalogProductId(shopId, item.getProductId()).isPresent();
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

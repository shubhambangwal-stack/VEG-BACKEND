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
 * REBUILT THIS ROUND -- the vendor-accept/reject broadcast redesign, matching
 * CustomerOrderService's rebuild. Two genuinely different lists now exist:
 * getOrderRequests (the broadcast inbox -- still-live candidates) and getShopOrders
 * (real order history -- only orders THIS shop actually won). Confirmed via live
 * testing that the old single-list model let a vendor who lost the accept race keep
 * seeing and acting on an order they never won -- see NOTES_VENDOR.md for the full
 * root-cause trace.
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

    /**
     * NEW THIS ROUND -- the broadcast inbox. Every order still awaiting a decision
     * that this shop can still act on (candidate, not yet accepted by anyone, hasn't
     * rejected it themselves). This is where accept()/reject() are meant to be called
     * from.
     */
    @Transactional(readOnly = true)
    public List<OrderResponseDto> getOrderRequests(UUID ownerUserId) {
        Shop shop = requireShop(ownerUserId);
        return customerOrderService.getOrderRequestsForShop(shop.getId());
    }

    /**
     * CHANGED MEANING THIS ROUND -- this is now real order history only: orders this
     * shop actually WON the accept race for. Previously returned every order this
     * shop was ever a candidate for, which is the exact bug this round fixes -- a
     * vendor who lost the race no longer sees the order here at all.
     */
    @Transactional(readOnly = true)
    public List<OrderResponseDto> getShopOrders(UUID ownerUserId) {
        Shop shop = requireShop(ownerUserId);
        return customerOrderService.getAcceptedOrdersForShop(shop.getId());
    }

    /**
     * Enriched single-order view for the Figma "Order Details" screen. Works for both
     * a pending request (viewing detail before deciding) and an already-accepted order
     * (management view) -- see requireShopOrder(). Items are FILTERED to only this
     * shop's own products -- an order can span multiple vendors, so subtotal/fee/total
     * below are scoped accordingly, not the full order's totalAmount. customerPhone
     * resolved live via UserLookupService (no phone denormalization needed -- always
     * fresh).
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

    /** BREAKING CHANGE THIS ROUND: acceptOrder now passes shop.getId() through -- CustomerOrderService.acceptOrder requires it to record who won the race. */
    @Transactional
    public void acceptOrder(UUID ownerUserId, UUID orderId) {
        Shop shop = requireShop(ownerUserId);
        customerOrderService.acceptOrder(orderId, shop.getId());
    }

    /**
     * BREAKING CHANGE THIS ROUND: rejectOrder now passes shop.getId() through, and no
     * longer cancels the whole order -- CustomerOrderService.rejectOrder narrows the
     * candidate pool instead (see that method's own javadoc). This shop stops seeing
     * the order in getOrderRequests either way; the order itself may stay live for
     * other candidates.
     */
    @Transactional
    public void rejectOrder(UUID ownerUserId, UUID orderId) {
        Shop shop = requireShop(ownerUserId);
        customerOrderService.rejectOrder(orderId, shop.getId());
    }

    /** Only valid on orders this shop actually won -- see requireAcceptedShopOrder(). */
    @Transactional
    public void updateOrderStatus(UUID ownerUserId, UUID orderId, String status) {
        Shop shop = requireShop(ownerUserId);
        requireAcceptedShopOrder(shop, orderId);
        customerOrderService.updateOrderStatus(orderId, status);
    }

    /**
     * NEW THIS ROUND -- the real dispatch trigger. Previously nothing in Vendor called
     * DeliveryDispatchService at all (only DeliveryTestController's /test/dispatch
     * exercised it). This is the vendor physically finishing prep and handing off to
     * delivery: flips the order to READY_FOR_PICKUP, then dispatches to Delivery,
     * which broadcasts to eligible partners within Admin's configured radius of THIS
     * shop's location and issues the pickup OTP once someone accepts (see Delivery's
     * NOTES_DELIVERY.md). Scoped to accepted orders only -- requireAcceptedShopOrder
     * throws if this shop never actually won the order (was only ever a candidate, or
     * lost the accept race).
     */
    @Transactional
    public String markReadyForPickup(UUID ownerUserId, UUID orderId) {
        Shop shop = requireShop(ownerUserId);
        OrderResponseDto order = requireAcceptedShopOrder(shop, orderId);

        if (shop.getLatitude() == null || shop.getLongitude() == null) {
            throw new BusinessException("VENDOR_SHOP_LOCATION_MISSING",
                    "Your shop's location isn't set -- update your shop profile before marking orders ready for pickup", HttpStatus.BAD_REQUEST);
        }

        customerOrderService.updateOrderStatus(orderId, "READY_FOR_PICKUP");

        deliveryDispatchService.dispatchOrder(orderId, order.getUserId(), ownerUserId, shop.getName(), shop.getAddress(),
                shop.getLatitude(), shop.getLongitude(), order.getLatitude(), order.getLongitude());

        return "Order marked ready for pickup -- nearby delivery partners are being notified";
    }

    /**
     * Shows the vendor the pickup OTP they need to hand over once a delivery partner
     * has accepted. Returns null (not an error) if nobody's accepted yet.
     */
    @Transactional(readOnly = true)
    public String getPickupOtp(UUID ownerUserId, UUID orderId) {
        Shop shop = requireShop(ownerUserId);
        requireAcceptedShopOrder(shop, orderId);
        return deliveryPickupInfoService.getPickupOtpForVendor(orderId, ownerUserId);
    }

    /** "Where's my delivery" for the vendor's own order-detail screen. */
    @Transactional(readOnly = true)
    public VendorDeliveryStatusDto getDeliveryStatus(UUID ownerUserId, UUID orderId) {
        Shop shop = requireShop(ownerUserId);
        requireAcceptedShopOrder(shop, orderId);
        return deliveryPickupInfoService.getDeliveryStatusForVendor(orderId, ownerUserId);
    }

    private Shop requireShop(UUID ownerUserId) {
        return shopRepository.findByOwnerUserIdAndDeletedAtIsNull(ownerUserId)
                .orElseThrow(() -> new BusinessException("VENDOR_SHOP_NOT_FOUND", "Shop not found", HttpStatus.NOT_FOUND));
    }

    /**
     * NEW THIS ROUND -- checks BOTH the pending-requests list and the accepted-orders
     * list, since order detail is meant to be viewable either before deciding
     * (accept/reject) or after winning it. Used by getOrderDetail() only -- anything
     * that performs a real post-accept ACTION (mark ready for pickup, status update,
     * pickup OTP, delivery status) must use requireAcceptedShopOrder() below instead,
     * which is intentionally stricter.
     */
    private OrderResponseDto requireShopOrder(Shop shop, UUID orderId) {
        var accepted = customerOrderService.getAcceptedOrdersForShop(shop.getId()).stream()
                .filter(o -> o.getId().equals(orderId))
                .findFirst();
        if (accepted.isPresent()) {
            return accepted.get();
        }

        return customerOrderService.getOrderRequestsForShop(shop.getId()).stream()
                .filter(o -> o.getId().equals(orderId))
                .findFirst()
                .orElseThrow(() -> new BusinessException("VENDOR_ORDER_NOT_FOUND", "Order not found for this shop", HttpStatus.NOT_FOUND));
    }

    /**
     * NEW THIS ROUND -- strict version: only orders this shop actually WON. This is
     * the check that fixes the root-cause bug -- a vendor who was merely a candidate
     * (or who lost the accept race to someone else) gets VENDOR_ORDER_NOT_FOUND here,
     * not a silently-successful action on an order that was never theirs.
     */
    private OrderResponseDto requireAcceptedShopOrder(Shop shop, UUID orderId) {
        return customerOrderService.getAcceptedOrdersForShop(shop.getId()).stream()
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

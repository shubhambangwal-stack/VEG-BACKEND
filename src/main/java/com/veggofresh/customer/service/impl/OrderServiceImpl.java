package com.veggofresh.customer.service.impl;

import com.veggofresh.auth.dto.UserSummaryDto;
import com.veggofresh.auth.service.UserLookupService;
import com.veggofresh.customer.dto.request.CartItemRequestDto;
import com.veggofresh.customer.dto.request.OrderRequestDto;
import com.veggofresh.customer.dto.request.RatingRequestDto;
import com.veggofresh.customer.dto.response.CartCheckoutBreakdownDto;
import com.veggofresh.customer.dto.response.CartResponseDto;
import com.veggofresh.customer.dto.response.CheckoutIssueDto;
import com.veggofresh.customer.dto.response.CheckoutResultDto;
import com.veggofresh.customer.dto.response.CheckoutSummaryDto;
import com.veggofresh.customer.dto.response.InvoiceDto;
import com.veggofresh.customer.dto.response.InvoiceLineItemDto;
import com.veggofresh.customer.dto.response.OrderItemResponseDto;
import com.veggofresh.customer.dto.response.OrderResponseDto;
import com.veggofresh.customer.dto.response.OrderTrackingResponseDto;
import com.veggofresh.customer.dto.response.RatingResponseDto;
import com.veggofresh.customer.dto.response.StatusTimelineDto;
import com.veggofresh.customer.entity.Address;
import com.veggofresh.customer.entity.Cart;
import com.veggofresh.customer.entity.CartItem;
import com.veggofresh.customer.entity.CustomerProfile;
import com.veggofresh.customer.entity.DeliverySlot;
import com.veggofresh.customer.entity.Order;
import com.veggofresh.customer.entity.OrderItem;
import com.veggofresh.customer.entity.OrderStatus;
import com.veggofresh.customer.entity.Rating;
import com.veggofresh.customer.repository.AddressRepository;
import com.veggofresh.customer.repository.CartRepository;
import com.veggofresh.customer.repository.CustomerProfileRepository;
import com.veggofresh.customer.repository.DeliverySlotRepository;
import com.veggofresh.customer.repository.OrderItemRepository;
import com.veggofresh.customer.repository.OrderRepository;
import com.veggofresh.customer.repository.RatingRepository;
import com.veggofresh.customer.service.CartService;
import com.veggofresh.customer.service.OrderService;
import com.veggofresh.payment.dto.PaymentHoldResponseDto;
import com.veggofresh.payment.service.PaymentService;
import com.veggofresh.payment.service.WalletService;
import com.veggofresh.payment.service.WalletTransactionReason;
import com.veggofresh.platform.exception.BusinessException;
import com.veggofresh.vendor.dto.ProductDto;
import com.veggofresh.vendor.service.ProductCatalogService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * PHASE 2 — NEW ARCHITECTURE, multi-cart / one-payment-to-N-orders checkout
 * (PROJECT_STATE section 2).
 *
 * VENDOR CATALOG PIVOT PATCH: ProductCatalogService methods now require a
 * latitude/longitude for radius eligibility. Every call site here uses a
 * location already in scope -- the resolved checkout Address in
 * checkout()/buildOrderFromCart()/getCheckoutSummary(), or the Order's own
 * stored delivery latitude/longitude in trackOrder()/getInvoice()/reorder().
 * No new address lookups were needed. See NOTES_CUSTOMER.md.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartRepository cartRepository;
    private final AddressRepository addressRepository;
    private final RatingRepository ratingRepository;
    private final DeliverySlotRepository deliverySlotRepository;
    private final CustomerProfileRepository customerProfileRepository;
    private final ProductCatalogService productCatalogService;
    private final CartService cartService;
    private final UserLookupService userLookupService;
    private final OrderResponseMapper orderResponseMapper;
    private final WalletService walletService;
    private final PaymentService paymentService;

    @Override
    public CheckoutResultDto checkout(UUID userId, OrderRequestDto request) {
        List<Cart> openCarts = cartRepository.findByUserIdOrderByCreatedAtAsc(userId);
        if (openCarts.isEmpty()) {
            throw new BusinessException("CART_EMPTY", "You have no items in any cart", HttpStatus.BAD_REQUEST);
        }

        Address address = addressRepository.findByIdAndUserId(request.getAddressId(), userId)
                .orElseThrow(() -> new BusinessException("ADDRESS_NOT_FOUND", "Invalid address selected", HttpStatus.BAD_REQUEST));

        DeliverySlot slot = null;
        if (request.getDeliverySlotId() != null) {
            slot = deliverySlotRepository.findById(request.getDeliverySlotId())
                    .orElseThrow(() -> new BusinessException("DELIVERY_SLOT_NOT_FOUND", "Selected delivery slot is invalid", HttpStatus.BAD_REQUEST));
        }

        List<OrderResponseDto> createdOrders = new ArrayList<>();
        List<CheckoutIssueDto> issues = new ArrayList<>();
        int cartIndex = 1;

        for (Cart cart : openCarts) {
            String cartLabel = "Cart " + cartIndex;

            if (cart.getItems() == null || cart.getItems().isEmpty()) {
                cartIndex++;
                continue;
            }

            // Re-validate vendor overlap fresh at checkout time — a cart's
            // overlap may have broken since add-time even if untouched
            // (PROJECT_STATE section 2, "Revisit-after-a-delay edge case").
            Set<UUID> liveIntersection = null;
            for (CartItem item : cart.getItems()) {
                Set<UUID> vendorsForItem = productCatalogService.getShopIdsForProduct(item.getProductId(), address.getLatitude(), address.getLongitude());
                liveIntersection = (liveIntersection == null)
                        ? new HashSet<>(vendorsForItem != null ? vendorsForItem : Set.of())
                        : intersect(liveIntersection, vendorsForItem != null ? vendorsForItem : Set.of());
            }

            if (liveIntersection == null || liveIntersection.isEmpty()) {
                issues.add(CheckoutIssueDto.builder()
                        .cartId(cart.getId())
                        .cartLabel(cartLabel)
                        .reason("Some items in this group are no longer available together — remove them to continue, or we'll leave this group out of your order")
                        .build());
                cartIndex++;
                continue;
            }

            Order order = buildOrderFromCart(userId, cart, address, slot, request, liveIntersection);
            Order saved = orderRepository.save(order);
            createdOrders.add(orderResponseMapper.mapToDto(saved));

            cartService.clearCart(userId, cart.getId());

            cartIndex++;
        }

        if (createdOrders.isEmpty()) {
            throw new BusinessException("CHECKOUT_FAILED", "None of your carts could be checked out — please review the issues", HttpStatus.BAD_REQUEST);
        }

        // PAYMENT INTEGRATION: create a single Razorpay order (hold) covering all
        // successfully checked-out orders. The frontend uses razorpayOrderId + razorpayKeyId
        // to open Razorpay Checkout.js. After the user pays, they call
        // POST /api/payment/orders/verify with the 3 values from Razorpay.
        List<UUID> orderIds = createdOrders.stream()
                .map(OrderResponseDto::getId)
                .collect(Collectors.toList());
        List<java.math.BigDecimal> orderAmounts = createdOrders.stream()
                .map(OrderResponseDto::getTotalAmount)
                .collect(Collectors.toList());
        PaymentHoldResponseDto paymentHold = paymentService.createHold(userId, orderIds, orderAmounts);

        log.info("Checkout complete: {} order(s) created, Razorpay order={}, total={}",
                createdOrders.size(), paymentHold.getRazorpayOrderId(), paymentHold.getTotalAmount());

        return CheckoutResultDto.builder()
                .orders(createdOrders)
                .issues(issues)
                .paymentHold(paymentHold)
                .build();
    }

    private Order buildOrderFromCart(UUID userId, Cart cart, Address address, DeliverySlot slot,
                                      OrderRequestDto request, Set<UUID> resolvedVendorIds) {
        Order order = new Order();
        order.setUserId(userId);
        order.setStatus(OrderStatus.PLACED);
        order.setDeliveryAddress(address.getAddressLine1() + ", " + address.getCity() + ", " + address.getState() + " - " + address.getPostalCode());
        order.setLatitude(address.getLatitude());
        order.setLongitude(address.getLongitude());
        order.setOrderNumber("#DM-" + (100000 + new Random().nextInt(900000)));
        order.setSourceCartId(cart.getId());
        order.setCandidateVendorIds(new HashSet<>(resolvedVendorIds));

        BigDecimal subtotal = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();

        for (CartItem item : cart.getItems()) {
            ProductDto product = productCatalogService.getProductById(item.getProductId(), address.getLatitude(), address.getLongitude());
            if (product == null) {
                throw new BusinessException("PRODUCT_NOT_FOUND", "One or more products in your cart are no longer available", HttpStatus.BAD_REQUEST);
            }

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProductId(item.getProductId());
            orderItem.setQuantity(item.getQuantity());
            orderItem.setPrice(product.getPrice());
            orderItem.setUnit(product.getUnit());
            orderItems.add(orderItem);

            subtotal = subtotal.add(product.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
        }
        order.setItems(orderItems);

        if (slot != null) {
            order.setDeliveryTimeSlot(slot.getLabel());
            order.setScheduledDate(request.getScheduledDate() != null ? LocalDate.parse(request.getScheduledDate()) : slot.getDate());
        }

        if (request.getPaymentMethodId() != null) {
            order.setPaymentMethodId(request.getPaymentMethodId().toString());
        }

        // PHASE 1 FIX: read the cart's real, already-validated promo instead
        // of hardcoding zero. Previously CouponService was injected here but
        // never actually called — promoDiscount was always ZERO regardless
        // of what the customer had applied in the cart.
        BigDecimal promoDiscount = cart.getPromoDiscount() != null ? cart.getPromoDiscount() : BigDecimal.ZERO;
        order.setPromoCode(cart.getPromoCode());

        BigDecimal deliveryFee = BigDecimal.valueOf(5.00);
        BigDecimal estimatedTax = subtotal.multiply(BigDecimal.valueOf(0.05));

        order.setDeliveryFee(deliveryFee);
        order.setEstimatedTax(estimatedTax);
        order.setPromoDiscount(promoDiscount);
        order.setTotalAmount(subtotal.add(deliveryFee).add(estimatedTax).subtract(promoDiscount));

        return order;
    }

    private Set<UUID> intersect(Set<UUID> a, Set<UUID> b) {
        Set<UUID> result = new HashSet<>(a);
        result.retainAll(b);
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponseDto> getOrderHistory(UUID userId, Pageable pageable) {
        Page<Order> orders = orderRepository.findByUserId(userId, pageable);
        List<OrderResponseDto> dtoList = orders.getContent().stream()
                .map(orderResponseMapper::mapToDto)
                .collect(Collectors.toList());
        return new PageImpl<>(dtoList, pageable, orders.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponseDto> getOrderHistoryByStatusGroup(UUID userId, String statusGroup, Pageable pageable) {
        Page<Order> orders;
        if ("IN_PROGRESS".equalsIgnoreCase(statusGroup)) {
            List<OrderStatus> inProgress = List.of(OrderStatus.PLACED, OrderStatus.CONFIRMED, OrderStatus.PREPARING, OrderStatus.OUT_FOR_DELIVERY);
            orders = orderRepository.findByUserIdAndStatusIn(userId, inProgress, pageable);
        } else if ("DELIVERED".equalsIgnoreCase(statusGroup)) {
            orders = orderRepository.findByUserIdAndStatus(userId, OrderStatus.DELIVERED, pageable);
        } else if ("CANCELLED".equalsIgnoreCase(statusGroup)) {
            orders = orderRepository.findByUserIdAndStatus(userId, OrderStatus.CANCELLED, pageable);
        } else {
            orders = orderRepository.findByUserId(userId, pageable);
        }

        List<OrderResponseDto> dtoList = orders.getContent().stream()
                .map(orderResponseMapper::mapToDto)
                .collect(Collectors.toList());
        return new PageImpl<>(dtoList, pageable, orders.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponseDto getOrderDetails(UUID userId, UUID orderId) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new BusinessException("ORDER_NOT_FOUND", "Order not found", HttpStatus.NOT_FOUND));
        return orderResponseMapper.mapToDto(order);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderTrackingResponseDto trackOrder(UUID userId, UUID orderId) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new BusinessException("ORDER_NOT_FOUND", "Order not found", HttpStatus.NOT_FOUND));

        List<StatusTimelineDto> timeline = new ArrayList<>();
        OrderStatus currentStatus = order.getStatus();

        timeline.add(StatusTimelineDto.builder()
                .step(1)
                .label("Placed")
                .completedAt(order.getCreatedAt())
                .isCurrent(currentStatus == OrderStatus.PLACED)
                .build());

        Instant preparingAt = order.getPreparingAt() != null ? order.getPreparingAt() :
                (currentStatus.ordinal() >= OrderStatus.CONFIRMED.ordinal() ? order.getCreatedAt().plus(5, ChronoUnit.MINUTES) : null);
        timeline.add(StatusTimelineDto.builder()
                .step(2)
                .label("Prepared")
                .completedAt(preparingAt)
                .isCurrent(currentStatus == OrderStatus.PREPARING || currentStatus == OrderStatus.CONFIRMED)
                .build());

        Instant outForDeliveryAt = order.getOutForDeliveryAt() != null ? order.getOutForDeliveryAt() :
                (currentStatus.ordinal() >= OrderStatus.OUT_FOR_DELIVERY.ordinal() ? order.getCreatedAt().plus(15, ChronoUnit.MINUTES) : null);
        timeline.add(StatusTimelineDto.builder()
                .step(3)
                .label("On the way")
                .completedAt(outForDeliveryAt)
                .isCurrent(currentStatus == OrderStatus.OUT_FOR_DELIVERY)
                .build());

        timeline.add(StatusTimelineDto.builder()
                .step(4)
                .label("Delivered")
                .completedAt(order.getDeliveredAt())
                .isCurrent(currentStatus == OrderStatus.DELIVERED)
                .build());

        List<OrderItemResponseDto> items = order.getItems().stream()
                .map(item -> {
                    ProductDto product = safeGetProduct(item.getProductId(), order.getLatitude(), order.getLongitude());
                    String name = product != null ? product.getName() : "Unknown Product";
                    return OrderItemResponseDto.builder()
                            .id(item.getId())
                            .productId(item.getProductId())
                            .productName(name)
                            .quantity(item.getQuantity())
                            .price(item.getPrice())
                            .unit(item.getUnit())
                            .subTotal(item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                            .build();
                })
                .collect(Collectors.toList());

        boolean rated = ratingRepository.findByOrderId(orderId).isPresent();

        return OrderTrackingResponseDto.builder()
                .orderId(order.getId())
                .orderNumber(order.getOrderNumber())
                .status(order.getStatus().name())
                .deliveryAddress(order.getDeliveryAddress())
                .estimatedDeliveryWindow(order.getEstimatedDeliveryWindow() != null ? order.getEstimatedDeliveryWindow() : "20-30 mins")
                .currentLatitude(order.getLatitude() + 0.001)
                .currentLongitude(order.getLongitude() - 0.001)
                .deliveryAgentName(order.getDeliveryAgentName() != null ? order.getDeliveryAgentName() : "John Veggie")
                .deliveryAgentPhone(order.getDeliveryAgentPhone() != null ? order.getDeliveryAgentPhone() : "+919876543222")
                .deliveryAgentPhotoUrl(order.getDeliveryAgentPhotoUrl())
                .statusTimeline(timeline)
                .items(items)
                .total(order.getTotalAmount())
                .deliveryPhotoUrl(order.getDeliveryPhotoUrl())
                .deliveryLocationNote(order.getDeliveryLocationNote())
                .deliveredAt(order.getDeliveredAt())
                .hasBeenRated(rated)
                .build();
    }

    @Override
    public RatingResponseDto rateOrder(UUID userId, UUID orderId, RatingRequestDto request) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new BusinessException("ORDER_NOT_FOUND", "Order not found", HttpStatus.NOT_FOUND));

        if (order.getStatus() != OrderStatus.DELIVERED) {
            throw new BusinessException("ORDER_NOT_DELIVERED", "You can only rate orders that have been delivered", HttpStatus.BAD_REQUEST);
        }

        ratingRepository.findByOrderId(orderId).ifPresent(r -> {
            throw new BusinessException("ORDER_ALREADY_RATED", "This order has already been rated", HttpStatus.BAD_REQUEST);
        });

        // NOTE: unchanged this round — still only writes to Rating, does not
        // orchestrate VendorShopRating/DeliveryPartnerRating (PROJECT_STATE
        // "rating fragmentation" gap). Out of scope for Phase 1/2, deferred
        // per the agreed round scope. See NOTES_CUSTOMER.md.
        Rating rating = new Rating();
        rating.setOrderId(orderId);
        rating.setRatingValue(request.getRatingValue());
        rating.setComment(request.getComment());

        Rating saved = ratingRepository.save(rating);
        return RatingResponseDto.builder()
                .id(saved.getId())
                .orderId(saved.getOrderId())
                .ratingValue(saved.getRatingValue())
                .comment(saved.getComment())
                .build();
    }

    @Override
    public OrderResponseDto updateOrderStatus(UUID orderId, OrderStatus newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException("ORDER_NOT_FOUND", "Order not found", HttpStatus.NOT_FOUND));

        if (!order.getStatus().isValidTransition(newStatus)) {
            throw new BusinessException("INVALID_ORDER_STATE_TRANSITION",
                    "Cannot transition order from status " + order.getStatus() + " to " + newStatus,
                    HttpStatus.BAD_REQUEST);
        }

        order.setStatus(newStatus);

        if (newStatus == OrderStatus.CONFIRMED) order.setConfirmedAt(Instant.now());
        else if (newStatus == OrderStatus.PREPARING) order.setPreparingAt(Instant.now());
        else if (newStatus == OrderStatus.OUT_FOR_DELIVERY) order.setOutForDeliveryAt(Instant.now());
        else if (newStatus == OrderStatus.DELIVERED) order.setDeliveredAt(Instant.now());
        else if (newStatus == OrderStatus.CANCELLED) order.setCancelledAt(Instant.now());

        return orderResponseMapper.mapToDto(orderRepository.save(order));
    }

    /**
     * WALLET WIRING (this round): a cancelled order now actually refunds the
     * customer -- order.getTotalAmount() is credited to their wallet. This applies
     * regardless of whether real payment collection exists yet (it doesn't -- Payment/
     * Razorpay integration is still unbuilt) so that the wallet ledger is already
     * correct and ready the moment checkout starts taking real payments. See
     * NOTES_CUSTOMER.md and Payment module's NOTES_PAYMENT.md for the full reasoning.
     */
    @Override
    public OrderResponseDto cancelOrder(UUID userId, UUID orderId) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new BusinessException("ORDER_NOT_FOUND", "Order not found", HttpStatus.NOT_FOUND));

        if (order.getStatus() != OrderStatus.PLACED && order.getStatus() != OrderStatus.CONFIRMED) {
            throw new BusinessException("ORDER_NOT_CANCELLABLE", "Can only cancel orders that are PLACED or CONFIRMED", HttpStatus.BAD_REQUEST);
        }

        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelledAt(Instant.now());
        Order saved = orderRepository.save(order);

        walletService.credit(userId, saved.getTotalAmount(), WalletTransactionReason.ORDER_CANCELLED_REFUND,
                saved.getId(), "Refund for cancelled order " + saved.getOrderNumber());

        return orderResponseMapper.mapToDto(saved);
    }

    @Override
    public List<CartResponseDto> reorder(UUID userId, UUID orderId) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new BusinessException("ORDER_NOT_FOUND", "Original order not found", HttpStatus.NOT_FOUND));

        List<CartResponseDto> result = null;
        for (OrderItem item : order.getItems()) {
            ProductDto product = productCatalogService.getProductById(item.getProductId(), order.getLatitude(), order.getLongitude());
            if (product == null) {
                throw new BusinessException("PRODUCT_NOT_AVAILABLE", "Some products from your previous order are no longer available", HttpStatus.BAD_REQUEST);
            }

            CartItemRequestDto req = new CartItemRequestDto();
            req.setProductId(item.getProductId());
            req.setQuantity(item.getQuantity());
            result = cartService.addItemToCart(userId, req);
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public InvoiceDto getInvoice(UUID userId, UUID orderId) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new BusinessException("ORDER_NOT_FOUND", "Order not found", HttpStatus.NOT_FOUND));

        UserSummaryDto user = userLookupService.findById(userId)
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND", "User profile not found in auth module"));

        // PHASE 1 FIX: use the customer's real display name — CustomerProfile
        // .fullName was already being stored via the profile endpoints but
        // never actually read here; this used to fall straight to phone.
        String customerName = customerProfileRepository.findByUserId(userId)
                .map(CustomerProfile::getFullName)
                .filter(name -> name != null && !name.isBlank())
                .orElse(user.getPhone());

        List<InvoiceLineItemDto> lineItems = order.getItems().stream()
                .map(item -> {
                    ProductDto product = safeGetProduct(item.getProductId(), order.getLatitude(), order.getLongitude());
                    String name = product != null ? product.getName() : "Unknown Product";
                    return InvoiceLineItemDto.builder()
                            .productName(name)
                            .quantity(item.getQuantity())
                            .unitPrice(item.getPrice())
                            .unit(item.getUnit())
                            .subTotal(item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                            .build();
                })
                .collect(Collectors.toList());

        BigDecimal subtotal = order.getItems().stream()
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return InvoiceDto.builder()
                .orderNumber(order.getOrderNumber())
                .orderDate(order.getCreatedAt().toString())
                .customerName(customerName)
                .customerEmail(user.getEmail())
                .customerPhone(user.getPhone())
                .deliveryAddress(order.getDeliveryAddress())
                .items(lineItems)
                .subtotal(subtotal)
                .deliveryFee(order.getDeliveryFee())
                .estimatedTax(order.getEstimatedTax())
                .promoDiscount(order.getPromoDiscount())
                .promoCode(order.getPromoCode())
                .total(order.getTotalAmount())
                .paymentMethod(order.getPaymentMethodId() != null ? "Credit Card" : "COD")
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public CheckoutSummaryDto getCheckoutSummary(UUID userId, UUID addressId) {
        Address address = addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new BusinessException("ADDRESS_NOT_FOUND", "Invalid address selected", HttpStatus.BAD_REQUEST));

        List<Cart> carts = cartRepository.findByUserIdOrderByCreatedAtAsc(userId);
        if (carts.isEmpty()) {
            throw new BusinessException("CART_EMPTY", "You have no items in any cart", HttpStatus.BAD_REQUEST);
        }

        List<CartCheckoutBreakdownDto> breakdowns = new ArrayList<>();
        int totalItemCount = 0;
        BigDecimal grandTotal = BigDecimal.ZERO;
        int index = 1;

        for (Cart cart : carts) {
            if (cart.getItems() == null || cart.getItems().isEmpty()) {
                index++;
                continue;
            }

            BigDecimal subtotal = BigDecimal.ZERO;
            int itemCount = cart.getItems().size();
            for (CartItem item : cart.getItems()) {
                ProductDto product = safeGetProduct(item.getProductId(), address.getLatitude(), address.getLongitude());
                if (product != null) {
                    subtotal = subtotal.add(product.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
                }
            }

            BigDecimal deliveryFee = BigDecimal.valueOf(5.00);
            BigDecimal estimatedTax = subtotal.multiply(BigDecimal.valueOf(0.05));
            // PHASE 1 FIX: read the cart's real promo instead of hardcoding zero.
            BigDecimal promoDiscount = cart.getPromoDiscount() != null ? cart.getPromoDiscount() : BigDecimal.ZERO;
            BigDecimal total = subtotal.add(deliveryFee).add(estimatedTax).subtract(promoDiscount);

            breakdowns.add(CartCheckoutBreakdownDto.builder()
                    .cartId(cart.getId())
                    .cartLabel("Cart " + index)
                    .itemCount(itemCount)
                    .subtotal(subtotal)
                    .deliveryFee(deliveryFee)
                    .estimatedTax(estimatedTax)
                    .promoDiscount(promoDiscount)
                    .promoCode(cart.getPromoCode())
                    .total(total)
                    .build());

            totalItemCount += itemCount;
            grandTotal = grandTotal.add(total);
            index++;
        }

        return CheckoutSummaryDto.builder()
                .carts(breakdowns)
                .totalItemCount(totalItemCount)
                .grandTotal(grandTotal)
                .build();
    }

    /**
     * Vendor's getProductById throws rather than returning null on
     * not-found/not-eligible (always has, before and after the catalog
     * pivot). Wrapping it here restores the graceful per-item skip several
     * `if (product != null)` checks in this class visually intended.
     */
    private ProductDto safeGetProduct(UUID productId, double latitude, double longitude) {
        try {
            return productCatalogService.getProductById(productId, latitude, longitude);
        } catch (Exception e) {
            return null;
        }
    }
}

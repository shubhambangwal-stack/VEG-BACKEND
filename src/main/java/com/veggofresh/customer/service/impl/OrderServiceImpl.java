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
import com.veggofresh.customer.service.CartVendorResolver;
import com.veggofresh.customer.service.OrderService;
import com.veggofresh.notification.entity.NotificationRecipientRole;
import com.veggofresh.notification.entity.NotificationType;
import com.veggofresh.notification.service.NotificationService;
import com.veggofresh.payment.dto.PaymentHoldResponseDto;
import com.veggofresh.payment.service.PaymentService;
import com.veggofresh.payment.service.WalletService;
import com.veggofresh.payment.service.WalletTransactionReason;
import com.veggofresh.platform.exception.BusinessException;
import com.veggofresh.vendor.dto.ProductDto;
import com.veggofresh.vendor.service.ProductCatalogService;
import com.veggofresh.vendor.service.ShopLookupService;
import com.veggofresh.admin.service.PlatformSettingsService;

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
import java.util.Locale;
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

    /**
     * Order numbers are derived from a random draw, so a single shared
     * {@link Random} is used rather than allocating one per order. The instance
     * is not thread-safe; it is guarded so concurrent checkouts cannot corrupt
     * its internal state (a plain {@code new Random()} per call was also
     * reseeded from the clock on every order, which made collisions likelier
     * under load, not less).
     */
    private static final Random ORDER_NUMBER_RANDOM = new Random();
    private static final Object ORDER_NUMBER_LOCK = new Object();

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
    private final NotificationService notificationService;
    private final ShopLookupService shopLookupService;
    private final PlatformSettingsService platformSettingsService;
    private final CartVendorResolver cartVendorResolver;

    @Override
    public CheckoutResultDto checkout(UUID userId, OrderRequestDto request) {
        Address address = addressRepository.findByIdAndUserId(request.getAddressId(), userId)
                .orElseThrow(() -> new BusinessException("ADDRESS_NOT_FOUND", "Invalid address selected",
                        HttpStatus.BAD_REQUEST));

        List<Cart> openCarts = shoppableCarts(userId);
        if (openCarts.isEmpty()) {
            throw new BusinessException("CART_EMPTY", "You have no items in any cart", HttpStatus.BAD_REQUEST);
        }

        DeliverySlot slot = null;
        if (request.getDeliverySlotId() != null) {
            slot = deliverySlotRepository.findById(request.getDeliverySlotId())
                    .orElseThrow(() -> new BusinessException("DELIVERY_SLOT_NOT_FOUND",
                            "Selected delivery slot is invalid", HttpStatus.BAD_REQUEST));
        }

        List<OrderResponseDto> createdOrders = new ArrayList<>();
        List<CheckoutIssueDto> issues = new ArrayList<>();
        List<Order> placedOrders = new ArrayList<>();

        for (int i = 0; i < openCarts.size(); i++) {
            Cart cart = openCarts.get(i);
            // One shared numbering, identical to the cart screen and to
            // getCheckoutSummary. Previously a local counter also advanced past
            // skipped carts, so a cart could be labelled "Cart 2" here and
            // "Cart 1" on the screen — and the client ties these together.
            String cartLabel = CartServiceImpl.cartLabel(i);

            // Re-validate vendor overlap fresh at checkout time — a cart's
            // overlap may have broken since add-time even if untouched
            // (PROJECT_STATE section 2, "Revisit-after-a-delay edge case").
            CartVendorResolver.Session vendors = newVendorSession(address);
            if (!vendors.revalidate(cart)) {
                issues.add(issue(cart, cartLabel,
                        "Some items in this group are no longer available together — remove them to continue, or we'll leave this group out of your order"));
                continue;
            }

            // A cart can pass the overlap check and still have nothing buyable
            // left (every product delisted or out of range). Building an order
            // from it would charge a delivery fee and platform fee for zero
            // items, so it is reported as an issue and skipped instead.
            List<CartItem> shippable = vendors.onlyShippableItems(cart);
            if (shippable.isEmpty()) {
                issues.add(issue(cart, cartLabel,
                        "None of the items in this group are available for delivery to this address — we'll leave this group out of your order"));
                continue;
            }

            Order order = buildOrderFromCart(userId, cart, address, slot, request, vendors.effectiveVendorIds(cart),
                    shippable);
            Order saved = orderRepository.save(order);
            createdOrders.add(orderResponseMapper.mapToDto(saved));
            placedOrders.add(saved);

            cartService.clearCart(userId, cart.getId());
        }

        if (createdOrders.isEmpty()) {
            throw new BusinessException("CHECKOUT_FAILED",
                    "None of your carts could be checked out — please review the issues", HttpStatus.BAD_REQUEST);
        }

        // PAYMENT INTEGRATION: create a single Razorpay order (hold) covering all
        // successfully checked-out orders. The frontend uses razorpayOrderId +
        // razorpayKeyId
        // to open Razorpay Checkout.js. After the user pays, they call
        // POST /api/payment/orders/verify with the 3 values from Razorpay.
        List<UUID> orderIds = createdOrders.stream()
                .map(OrderResponseDto::getId)
                .collect(Collectors.toList());
        List<java.math.BigDecimal> orderAmounts = createdOrders.stream()
                .map(OrderResponseDto::getTotalAmount)
                .collect(Collectors.toList());
        PaymentHoldResponseDto paymentHold = paymentService.createHold(userId, orderIds, orderAmounts);

        // Order-placed notifications fire only AFTER the payment hold succeeded
        // so they never outlive a rolled-back checkout.
        placedOrders.forEach(order -> notifyOrderPlaced(userId, order));

        log.info("Checkout complete: {} order(s) created, {} cart(s) skipped, razorpay order={}, total={}",
                createdOrders.size(), issues.size(), paymentHold.getRazorpayOrderId(), paymentHold.getTotalAmount());

        return CheckoutResultDto.builder()
                .orders(createdOrders)
                .issues(issues)
                .paymentHold(paymentHold)
                .build();
    }

    private CheckoutIssueDto issue(Cart cart, String cartLabel, String reason) {
        return CheckoutIssueDto.builder()
                .cartId(cart.getId())
                .cartLabel(cartLabel)
                .reason(reason)
                .build();
    }

    /**
     * Open carts that still hold at least one item, oldest first. Empty carts
     * are skipped so that they cannot consume a "Cart N" label and desync the
     * numbering from the cart screen. Writers should have retired them already
     * (see CartServiceImpl); this is the read-side guard.
     */
    private List<Cart> shoppableCarts(UUID userId) {
        return cartRepository.findByUserIdOrderByCreatedAtAscIdAsc(userId).stream()
                .filter(Cart::hasItems)
                .collect(Collectors.toList());
    }

    private CartVendorResolver.Session newVendorSession(Address address) {
        return cartVendorResolver.newSession(address.getLatitude(), address.getLongitude());
    }

    private Order buildOrderFromCart(UUID userId, Cart cart, Address address, DeliverySlot slot,
            OrderRequestDto request, Set<UUID> resolvedVendorIds, List<CartItem> shippableItems) {
        Order order = new Order();
        order.setUserId(userId);
        order.setStatus(OrderStatus.PLACED);
        order.setDeliveryAddress(address.getAddressLine1() + ", " + address.getCity() + ", " + address.getState()
                + " - " + address.getPostalCode());
        order.setLatitude(address.getLatitude());
        order.setLongitude(address.getLongitude());
        order.setOrderNumber(nextOrderNumber());
        order.setSourceCartId(cart.getId());
        order.setCandidateVendorIds(new HashSet<>(resolvedVendorIds));

        BigDecimal subtotal = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();

        // Only the shippable items are converted, and only for THIS cart. The
        // previous version threw on the first unresolvable product, which
        // propagated out of the whole checkout loop: one stale line in Cart 1
        // used to abort checkout for Cart 2 and Cart 3 as well, and the
        // customer got a 400 for carts that were perfectly fine.
        for (CartItem item : shippableItems) {
            ProductDto product = safeGetProduct(item.getProductId(), address.getLatitude(), address.getLongitude());
            if (product == null || product.getPrice() == null) {
                continue;
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
            order.setScheduledDate(
                    request.getScheduledDate() != null ? LocalDate.parse(request.getScheduledDate()) : slot.getDate());
        }

        if (request.getPaymentMethodId() != null) {
            order.setPaymentMethodId(request.getPaymentMethodId().toString());
        }

        // PHASE 1 FIX: read the cart's real, already-validated promo instead
        // of hardcoding zero. Previously CouponService was injected here but
        // never actually called — promoDiscount was always ZERO regardless
        // of what the customer had applied in the cart.
        BigDecimal promoDiscount = capAtSubtotal(
                cart.getPromoDiscount() != null ? cart.getPromoDiscount() : BigDecimal.ZERO, subtotal);
        order.setPromoCode(cart.getPromoCode());

        BigDecimal deliveryFee = platformSettingsService.getDeliveryFeeAmount();
        BigDecimal platformFee = platformSettingsService.getPlatformFeeAmount();
        BigDecimal estimatedTax = platformFee; // platform fee shown as tax line item

        order.setDeliveryFee(deliveryFee);
        order.setEstimatedTax(estimatedTax);
        order.setPromoDiscount(promoDiscount);

        BigDecimal total = subtotal.add(deliveryFee).add(estimatedTax).subtract(promoDiscount);
        // This total is what the payment hold is created against. A negative
        // amount here is unrecoverable downstream (a refund-direction hold), so
        // it is floored at zero rather than trusted.
        order.setTotalAmount(total.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : total);

        return order;
    }

    /** A discount can never exceed what it discounts. */
    private BigDecimal capAtSubtotal(BigDecimal discount, BigDecimal subtotal) {
        return discount.compareTo(subtotal) > 0 ? subtotal : discount;
    }

    private String nextOrderNumber() {
        synchronized (ORDER_NUMBER_LOCK) {
            return "#DM-" + (100000 + ORDER_NUMBER_RANDOM.nextInt(900000));
        }
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
            List<OrderStatus> inProgress = List.of(OrderStatus.PLACED, OrderStatus.CONFIRMED, OrderStatus.PREPARING,
                    OrderStatus.OUT_FOR_DELIVERY);
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

        Instant preparingAt = order.getPreparingAt() != null ? order.getPreparingAt()
                : (currentStatus.ordinal() >= OrderStatus.CONFIRMED.ordinal()
                        ? order.getCreatedAt().plus(5, ChronoUnit.MINUTES)
                        : null);
        timeline.add(StatusTimelineDto.builder()
                .step(2)
                .label("Prepared")
                .completedAt(preparingAt)
                .isCurrent(currentStatus == OrderStatus.PREPARING || currentStatus == OrderStatus.CONFIRMED)
                .build());

        Instant outForDeliveryAt = order.getOutForDeliveryAt() != null ? order.getOutForDeliveryAt()
                : (currentStatus.ordinal() >= OrderStatus.OUT_FOR_DELIVERY.ordinal()
                        ? order.getCreatedAt().plus(15, ChronoUnit.MINUTES)
                        : null);
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

        if ((order.getStatus() == OrderStatus.OUT_FOR_DELIVERY || order.getStatus() == OrderStatus.DELIVERED)
                && (order.getDropOtp() == null || order.getDropOtp().isBlank())) {
            String autoOtp = String.format("%06d", new java.security.SecureRandom().nextInt(1000000));
            order.setDropOtp(autoOtp);
            orderRepository.save(order);
        }

        // Vendor identity -- null until a shop has actually accepted this
        // order. FIXED THIS ROUND: deliveryAgentName/Phone below used to fall
        // back to a fake hardcoded agent ("John Veggie" / a made-up phone
        // number) whenever no real partner had been assigned yet -- that
        // fabricated a delivery person who doesn't exist. Now both stay null
        // until a real partner accepts, same as everywhere else.
        String shopName = null;
        String shopBusinessPhone = null;
        if (order.getAcceptedShopId() != null) {
            var shop = shopLookupService.findShopSummaryById(order.getAcceptedShopId()).orElse(null);
            if (shop != null) {
                shopName = shop.getName();
                shopBusinessPhone = shop.getBusinessPhone();
            }
        }

        return OrderTrackingResponseDto.builder()
                .orderId(order.getId())
                .orderNumber(order.getOrderNumber())
                .status(order.getStatus().name())
                .deliveryAddress(order.getDeliveryAddress())
                .estimatedDeliveryWindow(
                        order.getEstimatedDeliveryWindow() != null ? order.getEstimatedDeliveryWindow() : "20-30 mins")
                .currentLatitude(order.getLatitude() + 0.001)
                .currentLongitude(order.getLongitude() - 0.001)
                .shopName(shopName)
                .shopBusinessPhone(shopBusinessPhone)
                .deliveryAgentName(order.getDeliveryAgentName())
                .deliveryAgentPhone(order.getDeliveryAgentPhone())
                .deliveryAgentPhotoUrl(order.getDeliveryAgentPhotoUrl())
                .statusTimeline(timeline)
                .items(items)
                .total(order.getTotalAmount())
                .deliveryPhotoUrl(order.getDeliveryPhotoUrl())
                .deliveryLocationNote(order.getDeliveryLocationNote())
                .deliveredAt(order.getDeliveredAt())
                .hasBeenRated(rated)
                .dropOtp(order.getDropOtp())
                .build();
    }

    @Override
    @Transactional
    public String getDropOtp(UUID userId, UUID orderId) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new BusinessException("ORDER_NOT_FOUND", "Order not found", HttpStatus.NOT_FOUND));

        if ((order.getStatus() == OrderStatus.OUT_FOR_DELIVERY || order.getStatus() == OrderStatus.DELIVERED)
                && (order.getDropOtp() == null || order.getDropOtp().isBlank())) {
            String autoOtp = String.format("%06d", new java.security.SecureRandom().nextInt(1000000));
            order.setDropOtp(autoOtp);
            orderRepository.save(order);
        }
        return order.getDropOtp();
    }

    @Override
    public RatingResponseDto rateOrder(UUID userId, UUID orderId, RatingRequestDto request) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new BusinessException("ORDER_NOT_FOUND", "Order not found", HttpStatus.NOT_FOUND));

        if (order.getStatus() != OrderStatus.DELIVERED) {
            throw new BusinessException("ORDER_NOT_DELIVERED", "You can only rate orders that have been delivered",
                    HttpStatus.BAD_REQUEST);
        }

        ratingRepository.findByOrderId(orderId).ifPresent(r -> {
            throw new BusinessException("ORDER_ALREADY_RATED", "This order has already been rated",
                    HttpStatus.BAD_REQUEST);
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

        // REVIEW RECEIVED → the shop that actually fulfilled this order.
        UUID shopId = order.getAcceptedShopId();
        if (shopId != null) {
            shopLookupService.findOwnerUserIdByShopId(shopId).ifPresent(ownerId -> notificationService.send(ownerId,
                    NotificationRecipientRole.VENDOR, NotificationType.REVIEW_RECEIVED,
                    "New review received",
                    "A customer rated " + request.getRatingValue() + "/5 for order " + order.getOrderNumber(),
                    "{\"orderId\":\"" + orderId + "\",\"rating\":" + request.getRatingValue()
                            + ",\"comment\":"
                            + com.veggofresh.notification.util.NotificationJson.str(request.getComment()) + "}"));
        }

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

        if (newStatus == OrderStatus.CONFIRMED)
            order.setConfirmedAt(Instant.now());
        else if (newStatus == OrderStatus.PREPARING)
            order.setPreparingAt(Instant.now());
        else if (newStatus == OrderStatus.OUT_FOR_DELIVERY) {
            order.setOutForDeliveryAt(Instant.now());
            if (order.getDropOtp() == null || order.getDropOtp().isBlank()) {
                order.setDropOtp(String.format("%06d", new java.security.SecureRandom().nextInt(1000000)));
            }
        } else if (newStatus == OrderStatus.DELIVERED)
            order.setDeliveredAt(Instant.now());
        else if (newStatus == OrderStatus.CANCELLED)
            order.setCancelledAt(Instant.now());

        Order saved = orderRepository.save(order);

        notifyCustomerOrderStatusChange(saved, newStatus);

        return orderResponseMapper.mapToDto(saved);
    }

    /**
     * Notify the customer on every forward order-status change. This is the ONE
     * funnel every status change passes through (vendor updates via
     * CustomerOrderService, delivery pickup/delivery completion, system cancel),
     * so hooking here guarantees a single, consistent customer notification per
     * transition with no double-fires from the various callers.
     */
    private void notifyCustomerOrderStatusChange(Order order, OrderStatus newStatus) {
        NotificationType type;
        String title;
        switch (newStatus) {
            case PREPARING -> {
                type = NotificationType.ORDER_PACKED;
                title = "Your order is being prepared";
            }
            case READY_FOR_PICKUP -> {
                type = NotificationType.ORDER_READY_FOR_PICKUP;
                title = "Your order is packed & ready";
            }
            case OUT_FOR_DELIVERY -> {
                type = NotificationType.ORDER_OUT_FOR_DELIVERY;
                title = "Your order is out for delivery";
            }
            case DELIVERED -> {
                type = NotificationType.ORDER_DELIVERED;
                title = "Your order has been delivered";
            }
            case CONFIRMED -> {
                type = NotificationType.ORDER_CONFIRMED;
                title = "Your order was confirmed";
            }
            case CANCELLED -> {
                type = NotificationType.ORDER_CANCELLED;
                title = "Your order was cancelled";
            }
            default -> {
                return; // PLACED — nothing to announce
            }
        }
        notificationService.send(order.getUserId(), NotificationRecipientRole.CUSTOMER, type, title,
                "Order " + order.getOrderNumber() + " — " + title.toLowerCase(Locale.ROOT), orderData(order));
    }

    /**
     * Order placed: ping the customer (ORDER_PLACED) and every shop whose
     * candidate list the order was broadcast to (NEW_ORDER_REQUEST — the vendor
     * "new order inbox" ping).
     */
    private void notifyOrderPlaced(UUID customerUserId, Order saved) {
        notificationService.send(customerUserId, NotificationRecipientRole.CUSTOMER, NotificationType.ORDER_PLACED,
                "Order placed successfully",
                "Your order " + saved.getOrderNumber() + " has been placed — a nearby shop will confirm it shortly",
                orderData(saved));

        if (saved.getCandidateVendorIds() != null) {
            saved.getCandidateVendorIds()
                    .forEach(shopId -> shopLookupService.findOwnerUserIdByShopId(shopId)
                            .ifPresent(ownerId -> notificationService.send(ownerId, NotificationRecipientRole.VENDOR,
                                    NotificationType.NEW_ORDER_REQUEST,
                                    "New order request",
                                    "Order " + saved.getOrderNumber() + " is awaiting your shop's decision",
                                    orderData(saved))));
        }
    }

    private String orderData(Order order) {
        return "{\"orderId\":\"" + order.getId() + "\",\"orderNumber\":\"" + order.getOrderNumber() + "\"}";
    }

    /**
     * WALLET WIRING (this round): a cancelled order now actually refunds the
     * customer -- order.getTotalAmount() is credited to their wallet. This applies
     * regardless of whether real payment collection exists yet (it doesn't --
     * Payment/
     * Razorpay integration is still unbuilt) so that the wallet ledger is already
     * correct and ready the moment checkout starts taking real payments. See
     * NOTES_CUSTOMER.md and Payment module's NOTES_PAYMENT.md for the full
     * reasoning.
     */
    @Override
    public OrderResponseDto cancelOrder(UUID userId, UUID orderId) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new BusinessException("ORDER_NOT_FOUND", "Order not found", HttpStatus.NOT_FOUND));

        if (order.getStatus() != OrderStatus.PLACED && order.getStatus() != OrderStatus.CONFIRMED) {
            throw new BusinessException("ORDER_NOT_CANCELLABLE", "Can only cancel orders that are PLACED or CONFIRMED",
                    HttpStatus.BAD_REQUEST);
        }

        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelledAt(Instant.now());
        Order saved = orderRepository.save(order);

        notificationService.send(saved.getUserId(), NotificationRecipientRole.CUSTOMER,
                NotificationType.ORDER_CANCELLED,
                "Your order was cancelled",
                "Order " + saved.getOrderNumber() + " was cancelled — your refund is on the way",
                orderData(saved));

        walletService.credit(userId, saved.getTotalAmount(), WalletTransactionReason.ORDER_CANCELLED_REFUND,
                saved.getId(), "Refund for cancelled order " + saved.getOrderNumber());

        paymentService.onOrderCancelled(orderId);

        return orderResponseMapper.mapToDto(saved);
    }

    @Override
    public List<CartResponseDto> reorder(UUID userId, UUID orderId) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new BusinessException("ORDER_NOT_FOUND", "Original order not found",
                        HttpStatus.NOT_FOUND));

        if (order.getItems() == null || order.getItems().isEmpty()) {
            throw new BusinessException("ORDER_EMPTY", "This order has no items to reorder", HttpStatus.BAD_REQUEST);
        }

        // The whole reorder is one transaction, so a product that has since
        // become unbuyable rolls the entire re-add back rather than leaving the
        // customer with a half-populated cart that looks like a bug.
        for (OrderItem item : order.getItems()) {
            ProductDto product = productCatalogService.getProductById(item.getProductId(), order.getLatitude(),
                    order.getLongitude());
            if (product == null) {
                throw new BusinessException("PRODUCT_NOT_AVAILABLE",
                        "Some products from your previous order are no longer available", HttpStatus.BAD_REQUEST);
            }

            CartItemRequestDto req = new CartItemRequestDto();
            req.setProductId(item.getProductId());
            req.setQuantity(item.getQuantity());
            cartService.addItemToCart(userId, req);
        }

        // Previously this returned the result of the last addItemToCart call,
        // which is null for an order with no items — the client received a null
        // body inside a non-null ApiResponse wrapper.
        return cartService.getOpenCarts(userId);
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

        // Seller of record -- null until a shop has accepted this order.
        String shopName = null;
        String shopAddress = null;
        if (order.getAcceptedShopId() != null) {
            var shop = shopLookupService.findShopSummaryById(order.getAcceptedShopId()).orElse(null);
            if (shop != null) {
                shopName = shop.getName();
                shopAddress = shop.getAddress();
            }
        }

        return InvoiceDto.builder()
                .orderNumber(order.getOrderNumber())
                .orderDate(order.getCreatedAt().toString())
                .customerName(customerName)
                .customerEmail(user.getEmail())
                .customerPhone(user.getPhone())
                .shopName(shopName)
                .shopAddress(shopAddress)
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
                .orElseThrow(() -> new BusinessException("ADDRESS_NOT_FOUND", "Invalid address selected",
                        HttpStatus.BAD_REQUEST));

        List<Cart> carts = shoppableCarts(userId);
        if (carts.isEmpty()) {
            throw new BusinessException("CART_EMPTY", "You have no items in any cart", HttpStatus.BAD_REQUEST);
        }

        CartVendorResolver.Session vendors = newVendorSession(address);
        BigDecimal deliveryFee = platformSettingsService.getDeliveryFeeAmount();
        BigDecimal estimatedTax = platformSettingsService.getPlatformFeeAmount();

        List<CartCheckoutBreakdownDto> breakdowns = new ArrayList<>();
        List<CheckoutIssueDto> issues = new ArrayList<>();
        int totalItemCount = 0;
        BigDecimal grandTotal = BigDecimal.ZERO;

        for (int i = 0; i < carts.size(); i++) {
            Cart cart = carts.get(i);
            String cartLabel = CartServiceImpl.cartLabel(i);

            // Same two exclusions checkout() applies, evaluated up front so the
            // summary shows what will ACTUALLY be charged. A summary that
            // includes a cart checkout will then reject is worse than no
            // summary: the customer confirms a total that is wrong on submit.
            if (!vendors.revalidate(cart)) {
                issues.add(issue(cart, cartLabel,
                        "Some items in this group are no longer available together — this group will be left out of your order"));
                continue;
            }
            List<CartItem> shippable = vendors.onlyShippableItems(cart);
            if (shippable.isEmpty()) {
                issues.add(issue(cart, cartLabel,
                        "None of the items in this group are available for delivery to this address — this group will be left out of your order"));
                continue;
            }

            BigDecimal subtotal = BigDecimal.ZERO;
            int itemCount = 0;
            int unavailableItemCount = 0;
            for (CartItem item : cart.getItems()) {
                ProductDto product = safeGetProduct(item.getProductId(), address.getLatitude(), address.getLongitude());
                if (product == null || product.getPrice() == null) {
                    unavailableItemCount++;
                    continue;
                }
                // Sum of QUANTITIES, not the number of line items. This is the
                // value the cart screen and the badge already report; the
                // mismatch is exactly what made a two-cart customer see one
                // number on the cart page and a different one at checkout.
                itemCount += item.getQuantity();
                subtotal = subtotal.add(product.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
            }

            // PHASE 1 FIX: read the cart's real promo instead of hardcoding zero.
            BigDecimal promoDiscount = capAtSubtotal(
                    cart.getPromoDiscount() != null ? cart.getPromoDiscount() : BigDecimal.ZERO, subtotal);
            BigDecimal total = subtotal.add(deliveryFee).add(estimatedTax).subtract(promoDiscount);
            if (total.compareTo(BigDecimal.ZERO) < 0) {
                total = BigDecimal.ZERO;
            }

            breakdowns.add(CartCheckoutBreakdownDto.builder()
                    .cartId(cart.getId())
                    .cartLabel(cartLabel)
                    .itemCount(itemCount)
                    .unavailableItemCount(unavailableItemCount)
                    .subtotal(subtotal)
                    .deliveryFee(deliveryFee)
                    .estimatedTax(estimatedTax)
                    .promoDiscount(promoDiscount)
                    .promoCode(cart.getPromoCode())
                    .total(total)
                    .build());

            totalItemCount += itemCount;
            grandTotal = grandTotal.add(total);
        }

        if (breakdowns.isEmpty()) {
            throw new BusinessException("CART_EMPTY",
                    "None of your carts can be delivered to this address right now", HttpStatus.BAD_REQUEST);
        }

        return CheckoutSummaryDto.builder()
                .carts(breakdowns)
                .totalItemCount(totalItemCount)
                .grandTotal(grandTotal)
                .issues(issues)
                .build();
    }

    /**
     * Vendor's getProductById throws rather than returning null on
     * not-found/not-eligible (always has, before and after the catalog
     * pivot). Wrapping it here restores the graceful per-item skip several
     * `if (product != null)` checks in this class visually intended.
     *
     * ⚠️ Uses the non-throwing findEligibleProductById with NO try/catch on
     * purpose. Swallowing an exception raised inside a nested @Transactional
     * method still marks this transaction rollback-only, and the failure only
     * surfaces at commit as UnexpectedRollbackException -- long after the catch
     * block, with no clue which item caused it.
     */
    private ProductDto safeGetProduct(UUID productId, double latitude, double longitude) {
        return productCatalogService
                .findEligibleProductById(productId, latitude, longitude)
                .orElse(null);
    }
}

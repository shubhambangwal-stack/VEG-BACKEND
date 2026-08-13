package com.veggofresh.customer.service.impl;

import com.veggofresh.admin.service.CouponService;
import com.veggofresh.auth.dto.UserSummaryDto;
import com.veggofresh.auth.service.UserLookupService;
import com.veggofresh.customer.dto.request.OrderRequestDto;
import com.veggofresh.customer.dto.request.RatingRequestDto;
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
import com.veggofresh.customer.entity.DeliverySlot;
import com.veggofresh.customer.entity.Order;
import com.veggofresh.customer.entity.OrderItem;
import com.veggofresh.customer.entity.OrderStatus;
import com.veggofresh.customer.entity.Rating;
import com.veggofresh.customer.repository.AddressRepository;
import com.veggofresh.customer.repository.CartRepository;
import com.veggofresh.customer.repository.DeliverySlotRepository;
import com.veggofresh.customer.repository.OrderItemRepository;
import com.veggofresh.customer.repository.OrderRepository;
import com.veggofresh.customer.repository.RatingRepository;
import com.veggofresh.customer.service.CartService;
import com.veggofresh.customer.service.OrderService;
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
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

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
    private final ProductCatalogService productCatalogService;
    private final CartService cartService;
    private final UserLookupService userLookupService;
    private final CouponService couponService;

    @Override
    public OrderResponseDto checkout(UUID userId, OrderRequestDto request) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("CART_NOT_FOUND", "Cart not found for user", HttpStatus.NOT_FOUND));

        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new BusinessException("CART_EMPTY", "Cannot checkout an empty cart", HttpStatus.BAD_REQUEST);
        }

        Address address = addressRepository.findByIdAndUserId(request.getAddressId(), userId)
                .orElseThrow(() -> new BusinessException("ADDRESS_NOT_FOUND", "Invalid address selected", HttpStatus.BAD_REQUEST));

        // Create new Order
        Order order = new Order();
        order.setUserId(userId);
        order.setStatus(OrderStatus.PLACED);
        order.setDeliveryAddress(address.getAddressLine1() + ", " + address.getCity() + ", " + address.getState() + " - " + address.getPostalCode());
        order.setLatitude(address.getLatitude());
        order.setLongitude(address.getLongitude());

        // Generate Order Number
        String orderNumber = "#DM-" + (100000 + new Random().nextInt(900000));
        order.setOrderNumber(orderNumber);

        // Fetch items from cart and calculate subtotal
        BigDecimal subtotal = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();

        for (CartItem item : cart.getItems()) {
            ProductDto product = productCatalogService.getProductById(item.getProductId());
            if (product == null) {
                throw new BusinessException("PRODUCT_NOT_FOUND", "One or more products in your cart are no longer available", HttpStatus.BAD_REQUEST);
            }

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProductId(item.getProductId());
            orderItem.setQuantity(item.getQuantity());
            orderItem.setPrice(product.getPrice());
            orderItems.add(orderItem);

            subtotal = subtotal.add(product.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
        }

        order.setItems(orderItems);

        // Set delivery slot details if present
        if (request.getDeliverySlotId() != null) {
            DeliverySlot slot = deliverySlotRepository.findById(request.getDeliverySlotId())
                    .orElseThrow(() -> new BusinessException("DELIVERY_SLOT_NOT_FOUND", "Selected delivery slot is invalid", HttpStatus.BAD_REQUEST));
            order.setDeliveryTimeSlot(slot.getLabel());
            if (request.getScheduledDate() != null) {
                order.setScheduledDate(LocalDate.parse(request.getScheduledDate()));
            } else {
                order.setScheduledDate(slot.getDate());
            }
        }

        // Set payment details
        if (request.getPaymentMethodId() != null) {
            order.setPaymentMethodId(request.getPaymentMethodId().toString());
        }

        // Apply promo if any from cart/session
        BigDecimal promoDiscount = BigDecimal.ZERO;
        // In this implementation, we retrieve from cart's applied promo if available (stored or calculated here)
        // For simplicity, we check if cart matches any mock promo code. In a real system, the cart keeps promo status.
        // Let's assume we read from request or a default cart promo. Let's mock a promo:
        // We check if code "SAVE10" or "SAVE20" is applicable.
        // If checkout needs custom promo, it will be validated.
        
        // Fee computation
        BigDecimal deliveryFee = BigDecimal.valueOf(5.00); // flat delivery fee
        BigDecimal estimatedTax = subtotal.multiply(BigDecimal.valueOf(0.05)); // 5% tax

        order.setDeliveryFee(deliveryFee);
        order.setEstimatedTax(estimatedTax);
        order.setPromoDiscount(promoDiscount);
        order.setTotalAmount(subtotal.add(deliveryFee).add(estimatedTax).subtract(promoDiscount));

        // Save order
        Order savedOrder = orderRepository.save(order);

        // Clear cart after successful order placement
        cartService.clearCart(userId);

        return mapToDto(savedOrder);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponseDto> getOrderHistory(UUID userId, Pageable pageable) {
        Page<Order> orders = orderRepository.findByUserId(userId, pageable);
        List<OrderResponseDto> dtoList = orders.getContent().stream()
                .map(this::mapToDto)
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
                .map(this::mapToDto)
                .collect(Collectors.toList());
        return new PageImpl<>(dtoList, pageable, orders.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponseDto getOrderDetails(UUID userId, UUID orderId) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new BusinessException("ORDER_NOT_FOUND", "Order not found", HttpStatus.NOT_FOUND));
        return mapToDto(order);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderTrackingResponseDto trackOrder(UUID userId, UUID orderId) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new BusinessException("ORDER_NOT_FOUND", "Order not found", HttpStatus.NOT_FOUND));

        List<StatusTimelineDto> timeline = new ArrayList<>();
        OrderStatus currentStatus = order.getStatus();

        // 1. Placed
        timeline.add(StatusTimelineDto.builder()
                .step(1)
                .label("Placed")
                .completedAt(order.getCreatedAt())
                .isCurrent(currentStatus == OrderStatus.PLACED)
                .build());

        // 2. Preparing
        Instant preparingAt = order.getPreparingAt() != null ? order.getPreparingAt() : 
                (currentStatus.ordinal() >= OrderStatus.CONFIRMED.ordinal() ? order.getCreatedAt().plus(5, ChronoUnit.MINUTES) : null);
        timeline.add(StatusTimelineDto.builder()
                .step(2)
                .label("Prepared")
                .completedAt(preparingAt)
                .isCurrent(currentStatus == OrderStatus.PREPARING || currentStatus == OrderStatus.CONFIRMED)
                .build());

        // 3. Out for delivery
        Instant outForDeliveryAt = order.getOutForDeliveryAt() != null ? order.getOutForDeliveryAt() :
                (currentStatus.ordinal() >= OrderStatus.OUT_FOR_DELIVERY.ordinal() ? order.getCreatedAt().plus(15, ChronoUnit.MINUTES) : null);
        timeline.add(StatusTimelineDto.builder()
                .step(3)
                .label("On the way")
                .completedAt(outForDeliveryAt)
                .isCurrent(currentStatus == OrderStatus.OUT_FOR_DELIVERY)
                .build());

        // 4. Delivered
        timeline.add(StatusTimelineDto.builder()
                .step(4)
                .label("Delivered")
                .completedAt(order.getDeliveredAt())
                .isCurrent(currentStatus == OrderStatus.DELIVERED)
                .build());

        List<OrderItemResponseDto> items = order.getItems().stream()
                .map(item -> {
                    ProductDto product = productCatalogService.getProductById(item.getProductId());
                    String name = product != null ? product.getName() : "Unknown Product";
                    return OrderItemResponseDto.builder()
                            .id(item.getId())
                            .productId(item.getProductId())
                            .productName(name)
                            .quantity(item.getQuantity())
                            .price(item.getPrice())
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
                .currentLatitude(order.getLatitude() + 0.001) // simulated offset
                .currentLongitude(order.getLongitude() - 0.001) // simulated offset
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
        
        // record timestamps on transitions
        if (newStatus == OrderStatus.CONFIRMED) order.setConfirmedAt(Instant.now());
        else if (newStatus == OrderStatus.PREPARING) order.setPreparingAt(Instant.now());
        else if (newStatus == OrderStatus.OUT_FOR_DELIVERY) order.setOutForDeliveryAt(Instant.now());
        else if (newStatus == OrderStatus.DELIVERED) order.setDeliveredAt(Instant.now());
        else if (newStatus == OrderStatus.CANCELLED) order.setCancelledAt(Instant.now());

        return mapToDto(orderRepository.save(order));
    }

    @Override
    public OrderResponseDto cancelOrder(UUID userId, UUID orderId) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new BusinessException("ORDER_NOT_FOUND", "Order not found", HttpStatus.NOT_FOUND));

        if (order.getStatus() != OrderStatus.PLACED && order.getStatus() != OrderStatus.CONFIRMED) {
            throw new BusinessException("ORDER_NOT_CANCELLABLE", "Can only cancel orders that are PLACED or CONFIRMED", HttpStatus.BAD_REQUEST);
        }

        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelledAt(Instant.now());
        return mapToDto(orderRepository.save(order));
    }

    @Override
    public OrderResponseDto reorder(UUID userId, UUID orderId) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new BusinessException("ORDER_NOT_FOUND", "Original order not found", HttpStatus.NOT_FOUND));

        // Verify products availability
        Order newOrder = new Order();
        newOrder.setUserId(userId);
        newOrder.setStatus(OrderStatus.PLACED);
        newOrder.setDeliveryAddress(order.getDeliveryAddress());
        newOrder.setLatitude(order.getLatitude());
        newOrder.setLongitude(order.getLongitude());
        newOrder.setOrderNumber("#DM-" + (100000 + new Random().nextInt(900000)));

        BigDecimal subtotal = BigDecimal.ZERO;
        List<OrderItem> newItems = new ArrayList<>();

        for (OrderItem item : order.getItems()) {
            ProductDto product = productCatalogService.getProductById(item.getProductId());
            if (product == null) {
                throw new BusinessException("PRODUCT_NOT_AVAILABLE", "Some products from your previous order are no longer available", HttpStatus.BAD_REQUEST);
            }

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(newOrder);
            orderItem.setProductId(item.getProductId());
            orderItem.setQuantity(item.getQuantity());
            orderItem.setPrice(product.getPrice());
            newItems.add(orderItem);

            subtotal = subtotal.add(product.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
        }

        newOrder.setItems(newItems);
        newOrder.setDeliveryFee(order.getDeliveryFee() != null ? order.getDeliveryFee() : BigDecimal.valueOf(5.00));
        newOrder.setEstimatedTax(subtotal.multiply(BigDecimal.valueOf(0.05)));
        newOrder.setPromoDiscount(BigDecimal.ZERO);
        newOrder.setTotalAmount(subtotal.add(newOrder.getDeliveryFee()).add(newOrder.getEstimatedTax()));

        return mapToDto(orderRepository.save(newOrder));
    }

    @Override
    @Transactional(readOnly = true)
    public InvoiceDto getInvoice(UUID userId, UUID orderId) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new BusinessException("ORDER_NOT_FOUND", "Order not found", HttpStatus.NOT_FOUND));

        UserSummaryDto user = userLookupService.findById(userId)
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND", "User profile not found in auth module"));

        List<InvoiceLineItemDto> lineItems = order.getItems().stream()
                .map(item -> {
                    ProductDto product = productCatalogService.getProductById(item.getProductId());
                    String name = product != null ? product.getName() : "Unknown Product";
                    return InvoiceLineItemDto.builder()
                            .productName(name)
                            .quantity(item.getQuantity())
                            .unitPrice(item.getPrice())
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
                .customerName(user.getPhone()) // Fallback phone or email
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
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("CART_NOT_FOUND", "Cart not found for user", HttpStatus.NOT_FOUND));

        int itemCount = cart.getItems().size();
        BigDecimal subtotal = BigDecimal.ZERO;

        for (CartItem item : cart.getItems()) {
            ProductDto product = productCatalogService.getProductById(item.getProductId());
            if (product != null) {
                subtotal = subtotal.add(product.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
            }
        }

        BigDecimal deliveryFee = BigDecimal.valueOf(5.00); // flat
        BigDecimal estimatedTax = subtotal.multiply(BigDecimal.valueOf(0.05)); // 5% tax
        BigDecimal promoDiscount = BigDecimal.ZERO; // none applied yet

        return CheckoutSummaryDto.builder()
                .itemCount(itemCount)
                .subtotal(subtotal)
                .deliveryFee(deliveryFee)
                .estimatedTax(estimatedTax)
                .promoDiscount(promoDiscount)
                .total(subtotal.add(deliveryFee).add(estimatedTax).subtract(promoDiscount))
                .build();
    }

    private OrderResponseDto mapToDto(Order order) {
        List<OrderItemResponseDto> itemDtos = order.getItems().stream()
                .map(item -> {
                    ProductDto product = productCatalogService.getProductById(item.getProductId());
                    String name = product != null ? product.getName() : "Unknown Product";
                    return OrderItemResponseDto.builder()
                            .id(item.getId())
                            .productId(item.getProductId())
                            .productName(name)
                            .quantity(item.getQuantity())
                            .price(item.getPrice())
                            .subTotal(item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                            .build();
                })
                .collect(Collectors.toList());

        List<String> itemThumbnails = order.getItems().stream()
                .map(item -> productCatalogService.getProductById(item.getProductId()))
                .filter(p -> p != null && p.getImageUrl() != null)
                .map(ProductDto::getImageUrl)
                .limit(3)
                .collect(Collectors.toList());

        return OrderResponseDto.builder()
                .id(order.getId())
                .userId(order.getUserId())
                .orderNumber(order.getOrderNumber())
                .status(order.getStatus().name())
                .totalAmount(order.getTotalAmount())
                .deliveryFee(order.getDeliveryFee())
                .estimatedTax(order.getEstimatedTax())
                .promoDiscount(order.getPromoDiscount())
                .promoCode(order.getPromoCode())
                .deliveryAddress(order.getDeliveryAddress())
                .latitude(order.getLatitude())
                .longitude(order.getLongitude())
                .scheduledDate(order.getScheduledDate() != null ? order.getScheduledDate().toString() : null)
                .deliveryTimeSlot(order.getDeliveryTimeSlot())
                .paymentMethod(order.getPaymentMethodId() != null ? "Razorpay" : "COD")
                .paymentStatus(order.getPaymentStatus())
                .razorpayOrderId(order.getRazorpayOrderId())
                .itemCount(order.getItems().size())
                .itemThumbnails(itemThumbnails)
                .estimatedDeliveryWindow(order.getEstimatedDeliveryWindow())
                .canTrack(order.getStatus() == OrderStatus.OUT_FOR_DELIVERY)
                .canReorder(order.getStatus() == OrderStatus.DELIVERED)
                .canCancel(order.getStatus() == OrderStatus.PLACED || order.getStatus() == OrderStatus.CONFIRMED)
                .items(itemDtos)
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }
}

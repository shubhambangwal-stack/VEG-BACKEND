package com.veggofresh.customer.service.impl;

import com.veggofresh.customer.dto.request.OrderRequestDto;
import com.veggofresh.customer.dto.request.RatingRequestDto;
import com.veggofresh.customer.dto.response.OrderItemResponseDto;
import com.veggofresh.customer.dto.response.OrderResponseDto;
import com.veggofresh.customer.dto.response.OrderTrackingResponseDto;
import com.veggofresh.customer.dto.response.RatingResponseDto;
import com.veggofresh.customer.entity.Address;
import com.veggofresh.customer.entity.Cart;
import com.veggofresh.customer.entity.CartItem;
import com.veggofresh.customer.entity.Order;
import com.veggofresh.customer.entity.OrderItem;
import com.veggofresh.customer.entity.OrderStatus;
import com.veggofresh.customer.entity.Rating;
import com.veggofresh.customer.repository.AddressRepository;
import com.veggofresh.customer.repository.CartRepository;
import com.veggofresh.customer.repository.OrderItemRepository;
import com.veggofresh.customer.repository.OrderRepository;
import com.veggofresh.customer.repository.RatingRepository;
import com.veggofresh.customer.service.CartService;
import com.veggofresh.customer.service.OrderService;
import com.veggofresh.platform.exception.BusinessException;
import com.veggofresh.vendor.dto.ProductDto;
import com.veggofresh.vendor.service.ProductCatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartRepository cartRepository;
    private final AddressRepository addressRepository;
    private final RatingRepository ratingRepository;
    private final ProductCatalogService productCatalogService;
    private final CartService cartService;

    @Override
    public OrderResponseDto checkout(UUID userId, OrderRequestDto request) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("CART_NOT_FOUND", "Cart not found for user", HttpStatus.NOT_FOUND));

        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new BusinessException("CART_EMPTY", "Cannot checkout an empty cart", HttpStatus.BAD_REQUEST);
        }

        Address address = addressRepository.findByIdAndUserId(request.getAddressId(), userId)
                .orElseThrow(() -> new BusinessException("ADDRESS_NOT_FOUND", "Invalid address selected", HttpStatus.BAD_REQUEST));

        Order order = new Order();
        order.setUserId(userId);
        order.setStatus(OrderStatus.PLACED);
        order.setDeliveryAddress(address.getAddressLine1() + ", " + address.getCity() + ", " + address.getState() + " - " + address.getPostalCode());
        order.setLatitude(address.getLatitude());
        order.setLongitude(address.getLongitude());

        BigDecimal total = BigDecimal.ZERO;
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

            total = total.add(product.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
        }

        order.setTotalAmount(total);
        order.setItems(orderItems);

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

        return OrderTrackingResponseDto.builder()
                .orderId(order.getId())
                .status(order.getStatus().name())
                .currentLatitude(order.getLatitude() + 0.002) // simulated delivery offset
                .currentLongitude(order.getLongitude() - 0.002) // simulated delivery offset
                .deliveryAgentName("John Veggie")
                .deliveryAgentPhone("+919876543222")
                .estimatedDeliveryTime(Instant.now().plus(30, ChronoUnit.MINUTES))
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
        return mapToDto(orderRepository.save(order));
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

        return OrderResponseDto.builder()
                .id(order.getId())
                .userId(order.getUserId())
                .status(order.getStatus().name())
                .totalAmount(order.getTotalAmount())
                .deliveryAddress(order.getDeliveryAddress())
                .latitude(order.getLatitude())
                .longitude(order.getLongitude())
                .items(itemDtos)
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }
}

package com.veggofresh.customer.service;

import com.veggofresh.customer.dto.response.OrderItemResponseDto;
import com.veggofresh.customer.dto.response.OrderResponseDto;
import com.veggofresh.customer.entity.Order;
import com.veggofresh.customer.entity.OrderStatus;
import com.veggofresh.customer.repository.OrderRepository;
import com.veggofresh.customer.service.OrderService;
import com.veggofresh.vendor.dto.ProductDto;
import com.veggofresh.vendor.service.ProductCatalogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class    CustomerOrderServiceImpl implements CustomerOrderService {

    private final OrderRepository orderRepository;
    private final OrderService orderService;
    private final ProductCatalogService productCatalogService;

    @Override
    public void acceptOrder(UUID orderId) {
        log.info("Accepting order {}", orderId);
        orderService.updateOrderStatus(orderId, OrderStatus.CONFIRMED);
    }

    @Override
    public void rejectOrder(UUID orderId) {
        log.info("Rejecting order {}", orderId);
        orderService.updateOrderStatus(orderId, OrderStatus.CANCELLED);
    }

    @Override
    public void updateOrderStatus(UUID orderId, String status) {
        log.info("Updating order {} to status {}", orderId, status);
        OrderStatus newStatus = OrderStatus.valueOf(status.toUpperCase());
        orderService.updateOrderStatus(orderId, newStatus);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponseDto> getOrdersByShopId(UUID shopId) {
        log.info("Fetching orders for shop {}", shopId);
        List<Order> orders = orderRepository.findByShopId(shopId);
        return orders.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    public void assignDeliveryAgent(UUID orderId, String agentName, String agentPhone,
                                     String agentPhotoUrl, String estimatedWindow) {
        log.info("Assigning delivery agent {} to order {}", agentName, orderId);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new com.veggofresh.platform.exception.BusinessException("ORDER_NOT_FOUND", "Order not found"));
        order.setDeliveryAgentName(agentName);
        order.setDeliveryAgentPhone(agentPhone);
        order.setDeliveryAgentPhotoUrl(agentPhotoUrl);
        order.setEstimatedDeliveryWindow(estimatedWindow);
        orderRepository.save(order);
    }

    @Override
    public void markDelivered(UUID orderId, String deliveryPhotoUrl, String locationNote) {
        log.info("Marking order {} as delivered", orderId);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new com.veggofresh.platform.exception.BusinessException("ORDER_NOT_FOUND", "Order not found"));
        order.setDeliveryPhotoUrl(deliveryPhotoUrl);
        order.setDeliveryLocationNote(locationNote);
        order.setDeliveredAt(java.time.Instant.now());
        order.setStatus(OrderStatus.DELIVERED);
        orderRepository.save(order);
    }

    @Override
    @Transactional(readOnly = true)
    public String getDeliveryOtp(UUID orderId) {
        // Return a stable 4-digit code based on the order's UUID hash
        int code = Math.abs(orderId.hashCode() % 9000) + 1000;
        return String.valueOf(code);
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

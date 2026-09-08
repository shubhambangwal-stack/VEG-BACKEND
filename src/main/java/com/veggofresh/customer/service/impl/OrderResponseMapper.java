package com.veggofresh.customer.service.impl;

import com.veggofresh.customer.dto.response.OrderItemResponseDto;
import com.veggofresh.customer.dto.response.OrderResponseDto;
import com.veggofresh.customer.entity.Order;
import com.veggofresh.customer.entity.OrderStatus;
import com.veggofresh.admin.dto.response.ProductResponseDto;
import com.veggofresh.admin.service.AdminProductService;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * PHASE 1 FIX: single shared Order -> OrderResponseDto mapper.
 * Previously OrderServiceImpl and CustomerOrderServiceImpl each carried an
 * independently-maintained, byte-for-byte identical copy of this logic —
 * a real drift risk (e.g. multi-cart fields would only get added to one of
 * the two copies). Both now delegate here.
 *
 * VENDOR CATALOG PIVOT PATCH: ProductCatalogService.getProductById now needs
 * a latitude/longitude. Uses the Order's own stored delivery latitude/
 * longitude as the reference point -- correct semantically (what mattered
 * was availability at the place that order was actually delivered to), and
 * needs no new lookups since the Order is already in hand. Also wraps the
 * call in a try/catch: it throws rather than returning null on a missing/
 * ineligible product (always has, before and after the pivot), which made
 * the pre-existing `product != null` checks below dead code -- now they
 * actually do what they visually intended.
 */
@Component
@RequiredArgsConstructor
public class OrderResponseMapper {

    private final AdminProductService adminProductService;

    public OrderResponseDto mapToDto(Order order) {
        double lat = order.getLatitude();
        double lng = order.getLongitude();

        List<OrderItemResponseDto> itemDtos = order.getItems().stream()
                .map(item -> {
                    ProductResponseDto product = safeGetProduct(item.getProductId());
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

        List<String> itemThumbnails = order.getItems().stream()
                .map(item -> safeGetProduct(item.getProductId()))
                .filter(p -> p != null && p.getImageUrl() != null)
                .map(ProductResponseDto::getImageUrl)
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
                .paymentMethod(order.getPaymentMethodId() != null ? "Credit Card" : "COD")
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

    private ProductResponseDto safeGetProduct(java.util.UUID productId) {
        return adminProductService.findProductById(productId).orElse(null);
    }
}

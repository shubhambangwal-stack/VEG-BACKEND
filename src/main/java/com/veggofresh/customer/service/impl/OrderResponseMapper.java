package com.veggofresh.customer.service.impl;

import com.veggofresh.customer.dto.response.OrderItemResponseDto;
import com.veggofresh.customer.dto.response.OrderResponseDto;
import com.veggofresh.customer.entity.Order;
import com.veggofresh.customer.entity.OrderStatus;
import com.veggofresh.vendor.dto.ProductDto;
import com.veggofresh.vendor.service.ProductCatalogService;

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

    private final ProductCatalogService productCatalogService;

    public OrderResponseDto mapToDto(Order order) {
        double lat = order.getLatitude();
        double lng = order.getLongitude();

        List<OrderItemResponseDto> itemDtos = order.getItems().stream()
                .map(item -> {
                    ProductDto product = safeGetProduct(item.getProductId(), lat, lng);
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
                .map(item -> safeGetProduct(item.getProductId(), lat, lng))
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

    private ProductDto safeGetProduct(java.util.UUID productId, double lat, double lng) {
        try {
            return productCatalogService.getProductById(productId, lat, lng);
        } catch (Exception e) {
            return null;
        }
    }
}

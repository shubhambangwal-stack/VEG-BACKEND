package com.veggofresh.notification.service;

import com.veggofresh.notification.entity.NotificationRecipientRole;
import com.veggofresh.notification.entity.NotificationType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Convenience façade used by other modules (order, delivery, payment) to fire
 * domain-specific notifications without knowing the internals of
 * {@link NotificationService}. Each method maps a business event to the
 * correct {@link NotificationType} and {@link NotificationRecipientRole} and
 * delegates to the durable {@code send()} path.
 */
@Service
@RequiredArgsConstructor
public class NotificationSenderService {

    private final NotificationService notificationService;

    @Transactional
    public void sendOrderAcceptedNotification(UUID orderId, UUID vendorId, UUID customerId) {
        notificationService.send(
                customerId,
                NotificationRecipientRole.CUSTOMER,
                NotificationType.ORDER_ACCEPTED,
                "Order Accepted",
                "Your order has been accepted by the vendor",
                null
        );
    }

    @Transactional
    public void sendOrderStatusUpdate(UUID orderId, String newStatus, UUID customerId, UUID vendorId) {
        java.util.Map<String, String> statusMap = java.util.Map.of(
                "ORDER_PLACED",           "Order Placed",
                "ORDER_ACCEPTED",         "Order Accepted",
                "ORDER_REJECTED",         "Order Rejected",
                "ORDER_PREPARING",        "Order Preparing",
                "ORDER_OUT_FOR_DELIVERY", "Out for Delivery",
                "ORDER_DELIVERED",        "Order Delivered",
                "ORDER_CANCELLED",        "Order Cancelled"
        );
        String displayStatus = statusMap.getOrDefault(newStatus, newStatus);
        NotificationType type = resolveOrderStatusType(newStatus);
        notificationService.send(
                customerId,
                NotificationRecipientRole.CUSTOMER,
                type,
                "Order Status Update",
                "Your order status has been updated to " + displayStatus,
                null
        );
    }

    @Transactional
    public void sendVendorNewOrder(UUID orderId, String restaurantName, UUID vendorId) {
        notificationService.send(
                vendorId,
                NotificationRecipientRole.VENDOR,
                NotificationType.NEW_ORDER_REQUEST,
                "New Order Received",
                "You have received a new order from " + restaurantName,
                null
        );
    }

    @Transactional
    public void sendDeliveryAssignment(UUID assignmentId, String partnerName, UUID customerId) {
        notificationService.send(
                customerId,
                NotificationRecipientRole.CUSTOMER,
                NotificationType.DELIVERY_ASSIGNED,
                "New Delivery Assignment",
                partnerName + " has been assigned to your order",
                null
        );
    }

    @Transactional
    public void sendPickupOtp(UUID vendorId, UUID assignmentId, String otpCode) {
        notificationService.send(
                vendorId,
                NotificationRecipientRole.VENDOR,
                NotificationType.DELIVERY_PICKUP_OTP_GENERATED,
                "Pickup OTP",
                "Your pickup OTP is: " + otpCode + " (valid for 10 minutes)",
                null
        );
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private NotificationType resolveOrderStatusType(String rawStatus) {
        return switch (rawStatus) {
            case "ORDER_PLACED"           -> NotificationType.ORDER_PLACED;
            case "ORDER_ACCEPTED"         -> NotificationType.ORDER_ACCEPTED;
            case "ORDER_PREPARING"        -> NotificationType.ORDER_PACKED;
            case "ORDER_OUT_FOR_DELIVERY" -> NotificationType.ORDER_OUT_FOR_DELIVERY;
            case "ORDER_DELIVERED"        -> NotificationType.ORDER_DELIVERED;
            case "ORDER_CANCELLED"        -> NotificationType.ORDER_CANCELLED;
            default                       -> NotificationType.ORDER_PLACED;
        };
    }
}
package com.veggofresh.notification.service;

import com.veggofresh.notification.dto.NotificationSendRequestDto;
import com.veggofresh.notification.entity.Notification;
import com.veggofresh.notification.entity.Type;
import com.veggofresh.notification.entity.Notification.Status;
import com.veggofresh.notification.entity.Notification.Channel;
import com.veggofresh.notification.service.fcm.FcmService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
@RequiredArgsConstructor
public class NotificationSenderService {

    private final NotificationService notificationService;
    private final FcmService fcmService;

    @Transactional
    public Notification sendOrderAcceptedNotification(UUID orderId, UUID vendorId, UUID customerId) {
        String title = "Order Accepted";
        String message = "Your order has been accepted by the vendor";

        // Send in-app notification
        NotificationSendRequestDto inAppRequest = new NotificationSendRequestDto();
        inAppRequest.setRecipientId(customerId);
        inAppRequest.setRecipientType("CUSTOMER");
        inAppRequest.setNotificationType(Type.ORDER_ACCEPTED.name());
        inAppRequest.setTitle(title);
        inAppRequest.setMessage(message);
        inAppRequest.setPriority("HIGH");
        inAppRequest.setDeliveryChannel(Channel.IN_APP.name());
        notificationService.sendNotification(inAppRequest);

        // Send push notification to customer
        // TODO: In production, fetch customer's FCM token from registry
        // String fcmToken = fcmTokenRegistry.getToken(customerId, "CUSTOMER");
        // if (fcmToken != null) {
        //     fcmService.sendToToken(fcmToken, title, message, Map.of(
        //         "orderId", orderId.toString(),
        //         "type", "order_accepted"
        //     ));
        // }

        return null; // In-app only for now; push re-enable when token registry exists
    }

    @Transactional
    public Notification sendOrderStatusUpdate(UUID orderId, String newStatus, UUID customerId, UUID vendorId) {
        Map<String, String> statusMap = Map.of(
            "ORDER_PLACED", "Order Placed",
            "ORDER_ACCEPTED", "Order Accepted",
            "ORDER_REJECTED", "Order Rejected",
            "ORDER_PREPARING", "Order Preparing",
            "ORDER_OUT_FOR_DELIVERY", "Out for Delivery",
            "ORDER_DELIVERED", "Order Delivered",
            "ORDER_CANCELLED", "Order Cancelled"
        );

        String displayStatus = statusMap.getOrDefault(newStatus, newStatus);
        String title = "Order Status Update";
        String message = "Your order status has been updated to " + displayStatus;

        // In-app notification
        NotificationSendRequestDto inAppRequest = new NotificationSendRequestDto();
        inAppRequest.setRecipientId(customerId);
        inAppRequest.setRecipientType("CUSTOMER");
        inAppRequest.setNotificationType(Type.STATUS_UPDATE.name());
        inAppRequest.setTitle(title);
        inAppRequest.setMessage(message);
        inAppRequest.setPriority("HIGH");
        inAppRequest.setDeliveryChannel(Channel.IN_APP.name());
        notificationService.sendNotification(inAppRequest);

        return null;
    }

    @Transactional
    public Notification sendVendorNewOrder(UUID orderId, String restaurantName, UUID vendorId) {
        String title = "New Order Received";
        String message = "You have received a new order from " + restaurantName;

        // In-app notification to vendor
        NotificationSendRequestDto inAppRequest = new NotificationSendRequestDto();
        inAppRequest.setRecipientId(vendorId);
        inAppRequest.setRecipientType("VENDOR");
        inAppRequest.setNotificationType(Type.VENDOR_BROADCAST.name());
        inAppRequest.setTitle(title);
        inAppRequest.setMessage(message);
        inAppRequest.setPriority("HIGH");
        inAppRequest.setDeliveryChannel(Channel.IN_APP.name());
        notificationService.sendNotification(inAppRequest);

        // TODO: Send push to vendor's mobile app
        // String fcmToken = fcmTokenRegistry.getToken(vendorId, "VENDOR");
        // if (fcmToken != null) {
        //     fcmService.sendToToken(fcmToken, title, message, Map.of(
        //         "orderId", orderId.toString(),
        //         "type", "new_order"
        // ));
        // }

        return null;
    }

    @Transactional
    public Notification sendDeliveryAssignment(UUID assignmentId, String partnerName, UUID customerId) {
        String title = "New Delivery Assignment";
        String message = partnerName + " has been assigned to your order";

        // In-app notification
        NotificationSendRequestDto inAppRequest = new NotificationSendRequestDto();
        inAppRequest.setRecipientId(customerId);
        inAppRequest.setRecipientType("CUSTOMER");
        inAppRequest.setNotificationType(Type.DELIVERY_BROADCAST.name());
        inAppRequest.setTitle(title);
        inAppRequest.setMessage(message);
        inAppRequest.setPriority("HIGH");
        inAppRequest.setDeliveryChannel(Channel.IN_APP.name());
        notificationService.sendNotification(inAppRequest);

        // TODO: Send push to customer
        return null;
    }

    @Transactional
    public Notification sendPickupOtp(String vendorId, UUID assignmentId, String otpCode) {
        String title = "Pickup OTP";
        String message = "Your pickup OTP is: " + otpCode + " (valid for 10 minutes)";

        // In-app notification to delivery partner
        NotificationSendRequestDto inAppRequest = new NotificationSendRequestDto();
        inAppRequest.setRecipientId(vendorId); // vendor issuing OTP
        inAppRequest.setRecipientType("VENDOR");
        inAppRequest.setNotificationType(Type.PICKUP_OTP.name());
        inAppRequest.setTitle(title);
        inAppRequest.setMessage(message);
        inAppRequest.setPriority("HIGH");
        inAppRequest.setDeliveryChannel(Channel.IN_APP.name());
        notificationService.sendNotification(inAppRequest);

        // Push to delivery partner (TODO: fetch token)
        // String fcmToken = fcmTokenRegistry.getToken(vendorId, "DELIVERY_PARTNER");
        // if (fcmToken != null) {
        //     fcmService.sendToToken(fcmToken, title, message, Map.of(
         //       "assignmentId", assignmentId.toString(),
          //      "otp", otpCode
          //  ));
        // }

        return null;
    }
}
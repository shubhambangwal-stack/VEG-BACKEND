package com.veggofresh.notification.service.fcm;

import com.veggofresh.notification.entity.Notification;
import com.veggofresh.platform.common.BaseEntity;
import com.google.firebase.messaging.*;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class FcmService {

    @Value("${fcm.server.key}")
    private String serverKey;

    private FirebaseMessagingClient fcmClient;

    @PostConstruct
    public void init() {
        if (serverKey != null && !serverKey.isEmpty()) {
            this.fcmClient = new FirebaseMessagingClient(serverKey);
        }
    }

    /**
     * Send push notification to a specific device token
     */
    @Transactional
    public String sendToToken(String token, String title, String body, Map<String, String> data) throws Exception {
        if (fcmClient == null) {
            // FCM not configured - log and return null
            // In production, this would be monitored
            return null;
        }

        Message message = Message.builder()
                .setToken(token)
                .setNotification(Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build())
                .putAllData(data)
                .setApnsConfig(ApnsConfig.builder()
                    .setAps(Aps.builder()
                        .setCategory("ORDER_CATEGORY")
                        .build())
                    .build())
                .setAndroidConfig(AndroidConfig.builder()
                    .setPriority(AndroidConfig.Priority.HIGH)
                    .setNotification(AndroidNotification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .setSound("default")
                        .setChannelId("default")
                        .build())
                    .build())
                .build();

        return fcmClient.sendAsync(message).get();
    }

    /**
     * Send push notification to a topic (for broadcast to multiple devices)
     */
    @Transactional
    public String sendToTopic(String topic, String title, String body, Map<String, String> data) throws Exception {
        if (fcmClient == null) {
            return null;
        }

        Message message = Message.builder()
                .setTopic(topic)
                .setNotification(Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build())
                .putAllData(data)
                .build();

        return fcmClient.sendAsync(message).get();
    }

    /**
     * Send multicast to multiple tokens (up to 500 at once)
     */
    @Transactional
    public String sendToMultipleTokens(Collection<String> tokens, String title, String body, Map<String, String> data) throws Exception {
        if (fcmClient == null) {
            return null;
        }

        MulticastMessage message = MulticastMessage.builder()
                .addAllTokens(new ArrayList<>(tokens))
                .setNotification(Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build())
                .putAllData(data)
                .build();

        return fcmClient.sendAsync(message).get();
    }

    /**
     * Subscribe a device token to a topic
     */
    @Transactional
    public String subscribeToTopic(String token, String topic) throws Exception {
        if (fcmClient == null) {
            return null;
        }

        SubscribeToTopicRequest request = SubscribeToTopicRequest.builder()
                .setTopicName(topic)
                .setToken(token)
                .build();

        return fcmClient.sendAsync(request).get();
    }

    /**
     * Unsubscribe a device token from a topic
     */
    @Transactional
    public String unsubscribeFromTopic(String token, String topic) throws Exception {
        if (fcmClient == null) {
            return null;
        }

        UnsubscribeFromTopicRequest request = UnsubscribeFromTopicRequest.builder()
                .setTopicName(topic)
                .setToken(token)
                .build();

        return fcmClient.sendAsync(request).get();
    }
}
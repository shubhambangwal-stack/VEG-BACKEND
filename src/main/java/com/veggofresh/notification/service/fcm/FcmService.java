package com.veggofresh.notification.service.fcm;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.AndroidNotification;
import com.google.firebase.messaging.ApnsConfig;
import com.google.firebase.messaging.Aps;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.SendResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class FcmService {

    public boolean isInitialized() {
        return !FirebaseApp.getApps().isEmpty();
    }

    public String sendToToken(String token, String title, String body, Map<String, String> data) {
        if (!isInitialized() || token == null || token.isBlank()) {
            return null;
        }

        Map<String, String> safeData = sanitizeDataMap(data);

        Message message = Message.builder()
                .setToken(token)
                .setNotification(com.google.firebase.messaging.Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build())
                .putAllData(safeData)
                .setApnsConfig(ApnsConfig.builder()
                        .setAps(Aps.builder()
                                .setCategory("ORDER_CATEGORY")
                                .setSound("default")
                                .build())
                        .build())
                .setAndroidConfig(AndroidConfig.builder()
                        .setPriority(AndroidConfig.Priority.HIGH)
                        .setNotification(AndroidNotification.builder()
                                .setTitle(title)
                                .setBody(body)
                                .setSound("default")
                                .setChannelId("veggofresh_notifications")
                                .build())
                        .build())
                .build();

        try {
            String messageId = FirebaseMessaging.getInstance().sendAsync(message).get();
            log.debug("FCM push sent successfully to token (messageId: {})", messageId);
            return messageId;
        } catch (Exception e) {
            log.warn("Failed to send FCM push to token: {}", e.getMessage());
            return null;
        }
    }

    public String sendToTopic(String topic, String title, String body, Map<String, String> data) {
        if (!isInitialized() || topic == null || topic.isBlank()) {
            return null;
        }

        Map<String, String> safeData = sanitizeDataMap(data);

        Message message = Message.builder()
                .setTopic(topic)
                .setNotification(com.google.firebase.messaging.Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build())
                .putAllData(safeData)
                .setAndroidConfig(AndroidConfig.builder()
                        .setPriority(AndroidConfig.Priority.HIGH)
                        .setNotification(AndroidNotification.builder()
                                .setTitle(title)
                                .setBody(body)
                                .setSound("default")
                                .setChannelId("veggofresh_announcements")
                                .build())
                        .build())
                .build();

        try {
            String messageId = FirebaseMessaging.getInstance().sendAsync(message).get();
            log.info("FCM push sent successfully to topic '{}' (messageId: {})", topic, messageId);
            return messageId;
        } catch (Exception e) {
            log.warn("Failed to send FCM push to topic '{}': {}", topic, e.getMessage());
            return null;
        }
    }

    /**
     * Sends notification to multiple tokens and returns a list of invalid/unregistered tokens
     * that should be removed from the database.
     */
    public List<String> sendToMultipleTokens(Collection<String> tokens, String title, String body, Map<String, String> data) {
        if (!isInitialized() || tokens == null || tokens.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> tokenList = new ArrayList<>(tokens);
        Map<String, String> safeData = sanitizeDataMap(data);

        MulticastMessage message = MulticastMessage.builder()
                .addAllTokens(tokenList)
                .setNotification(com.google.firebase.messaging.Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build())
                .putAllData(safeData)
                .setApnsConfig(ApnsConfig.builder()
                        .setAps(Aps.builder()
                                .setCategory("ORDER_CATEGORY")
                                .setSound("default")
                                .build())
                        .build())
                .setAndroidConfig(AndroidConfig.builder()
                        .setPriority(AndroidConfig.Priority.HIGH)
                        .setNotification(AndroidNotification.builder()
                                .setTitle(title)
                                .setBody(body)
                                .setSound("default")
                                .setChannelId("veggofresh_notifications")
                                .build())
                        .build())
                .build();

        List<String> invalidTokens = new ArrayList<>();
        try {
            BatchResponse response = FirebaseMessaging.getInstance().sendEachForMulticastAsync(message).get();
            log.info("FCM multicast result: {} successful, {} failed out of {} tokens",
                    response.getSuccessCount(), response.getFailureCount(), tokenList.size());

            if (response.getFailureCount() > 0) {
                List<SendResponse> responses = response.getResponses();
                for (int i = 0; i < responses.size(); i++) {
                    if (!responses.get(i).isSuccessful()) {
                        FirebaseMessagingException exception = responses.get(i).getException();
                        if (exception != null && (exception.getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED
                                || exception.getMessagingErrorCode() == MessagingErrorCode.INVALID_ARGUMENT)) {
                            invalidTokens.add(tokenList.get(i));
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to execute FCM multicast push: {}", e.getMessage());
        }
        return invalidTokens;
    }

    public String subscribeToTopic(String token, String topic) {
        if (!isInitialized() || token == null || token.isBlank() || topic == null || topic.isBlank()) {
            return null;
        }

        try {
            FirebaseMessaging.getInstance().subscribeToTopicAsync(List.of(token), topic).get();
            return topic;
        } catch (Exception e) {
            log.warn("Failed to subscribe token to topic {}: {}", topic, e.getMessage());
            return null;
        }
    }

    public String unsubscribeFromTopic(String token, String topic) {
        if (!isInitialized() || token == null || token.isBlank() || topic == null || topic.isBlank()) {
            return null;
        }

        try {
            FirebaseMessaging.getInstance().unsubscribeFromTopicAsync(List.of(token), topic).get();
            return topic;
        } catch (Exception e) {
            log.warn("Failed to unsubscribe token from topic {}: {}", topic, e.getMessage());
            return null;
        }
    }

    private Map<String, String> sanitizeDataMap(Map<String, String> data) {
        if (data == null) {
            return Collections.emptyMap();
        }
        Map<String, String> safeMap = new HashMap<>();
        data.forEach((key, val) -> {
            if (key != null && val != null) {
                safeMap.put(key, val);
            }
        });
        return safeMap;
    }
}
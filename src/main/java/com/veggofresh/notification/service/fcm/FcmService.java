package com.veggofresh.notification.service.fcm;

import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.AndroidNotification;
import com.google.firebase.messaging.ApnsConfig;
import com.google.firebase.messaging.Aps;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MulticastMessage;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@Service
public class FcmService {

    public String sendToToken(String token, String title, String body, Map<String, String> data) throws Exception {
        if (token == null || token.isEmpty()) {
            return null;
        }

        Message message = Message.builder()
                .setToken(token)
                .setNotification(com.google.firebase.messaging.Notification.builder()
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

        return FirebaseMessaging.getInstance().sendAsync(message).get();
    }

    public String sendToTopic(String topic, String title, String body, Map<String, String> data) throws Exception {
        if (topic == null || topic.isEmpty()) {
            return null;
        }

        Message message = Message.builder()
                .setTopic(topic)
                .setNotification(com.google.firebase.messaging.Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build())
                .putAllData(data)
                .build();

        return FirebaseMessaging.getInstance().sendAsync(message).get();
    }

    public String sendToMultipleTokens(Collection<String> tokens, String title, String body, Map<String, String> data) throws Exception {
        if (tokens == null || tokens.isEmpty()) {
            return null;
        }

        MulticastMessage message = MulticastMessage.builder()
                .addAllTokens(new ArrayList<>(tokens))
                .setNotification(com.google.firebase.messaging.Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build())
                .putAllData(data)
                .build();

        FirebaseMessaging.getInstance().sendMulticastAsync(message).get();
        return null;
    }

    public String subscribeToTopic(String token, String topic) throws Exception {
        if (token == null || token.isEmpty() || topic == null || topic.isEmpty()) {
            return null;
        }

        FirebaseMessaging.getInstance().subscribeToTopicAsync(List.of(token), topic).get();
        return topic;
    }

    public String unsubscribeFromTopic(String token, String topic) throws Exception {
        if (token == null || token.isEmpty() || topic == null || topic.isEmpty()) {
            return null;
        }

        FirebaseMessaging.getInstance().unsubscribeFromTopicAsync(List.of(token), topic).get();
        return topic;
    }
}
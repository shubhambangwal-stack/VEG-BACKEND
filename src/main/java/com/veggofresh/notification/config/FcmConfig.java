package com.veggofresh.notification.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FcmConfig {

    @Value("${fcm.server.key}")
    private String serverKey;

    @PostConstruct
    public void init() {
        // FCM initialized lazily via Firebase Admin SDK
    }
}
package com.veggofresh.notification.config;

import com.veggofresh.notification.service.fcm.FcmService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;

@Configuration
@ConditionalOnProperty(name = "fcm.server.key", havingValue = "", matchIfMissing = false)
@Import(FcmService.class)
public class FcmConfig {

    @Value("${fcm.server.key}")
    private String serverKey;

    @Bean
    public FcmService fcmService() {
        return new FcmService(serverKey);
    }

    /**
     * Custom bean to lazily initialize Firebase App if needed.
     * Firebase Admin SDK requires initialization with a service account key.
     * This bean conditionally initializes based on environment.
     */
    @Bean
    @ConditionalOnMissingBean
    public FirebaseOptions firebaseOptions() throws IOException {
        // In production, you would load from a service account JSON file
        // For now, we use default initialization which works with Firebase Console setup
        return FirebaseOptions.getInstance();
    }
}
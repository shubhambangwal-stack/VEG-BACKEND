package com.veggofresh.notification.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.FileInputStream;
import java.io.InputStream;

/**
 * Initializes the Firebase Admin SDK (used for FCM push and Firebase Auth token
 * verification / phone auth).
 *
 * <p>The service account JSON (downloaded from the Firebase console) is supplied via
 * the {@code veggofresh.firebase.service-account-path} property (env
 * {@code FIREBASE_SERVICE_ACCOUNT_PATH}). The value may be a filesystem path or a
 * {@code classpath:} prefixed resource (e.g. {@code classpath:service-account.json}).
 * When the path is blank, Firebase is skipped so local development without credentials
 * still boots.
 */
@Slf4j
@Configuration
public class FcmConfig {

    @Value("${veggofresh.firebase.service-account-path:}")
    private String serviceAccountPath;

    @PostConstruct
    public void init() {
        if (serviceAccountPath == null || serviceAccountPath.isBlank()) {
            log.warn("FIREBASE_SERVICE_ACCOUNT_PATH not set — Firebase Admin SDK not initialized");
            return;
        }

        try (InputStream serviceAccount = openServiceAccount(serviceAccountPath)) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
                log.info("Firebase Admin SDK initialized successfully");
            }
        } catch (Exception e) {
            log.error("Failed to initialize Firebase Admin SDK", e);
        }
    }

    private InputStream openServiceAccount(String path) throws Exception {
        if (path.startsWith("classpath:")) {
            Resource resource = new ClassPathResource(path.substring("classpath:".length()));
            return resource.getInputStream();
        }
        return new FileInputStream(path);
    }
}

package com.veggofresh.notification.controller;

import com.veggofresh.notification.dto.DeviceTokenRequestDto;
import com.veggofresh.notification.entity.UserDeviceToken;
import com.veggofresh.notification.repository.UserDeviceTokenRepository;
import com.veggofresh.platform.common.ApiResponse;
import com.veggofresh.platform.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Endpoints for mobile (Flutter / Android / iOS) and Web clients to register,
 * update, and unregister FCM device tokens for push notification delivery.
 */
@Slf4j
@RestController
@RequestMapping("/api/notifications/device-token")
@RequiredArgsConstructor
public class DeviceTokenController {

    private final UserDeviceTokenRepository deviceTokenRepository;

    @PostMapping
    @Transactional
    public ResponseEntity<ApiResponse<Void>> registerDeviceToken(@Valid @RequestBody DeviceTokenRequestDto request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        String token = request.getToken().trim();
        String platform = (request.getPlatform() == null || request.getPlatform().isBlank())
                ? "ANDROID" : request.getPlatform().trim().toUpperCase();

        Optional<UserDeviceToken> existing = deviceTokenRepository.findByFcmToken(token);
        UserDeviceToken deviceToken;
        if (existing.isPresent()) {
            deviceToken = existing.get();
            deviceToken.setUserId(userId);
            deviceToken.setPlatform(platform);
            deviceToken.setLastUsedAt(Instant.now());
        } else {
            deviceToken = new UserDeviceToken();
            deviceToken.setUserId(userId);
            deviceToken.setFcmToken(token);
            deviceToken.setPlatform(platform);
            deviceToken.setLastUsedAt(Instant.now());
        }
        deviceTokenRepository.save(deviceToken);
        log.info("Registered FCM device token for user {} (platform: {})", userId, platform);

        return ResponseEntity.ok(ApiResponse.success("Device token registered successfully"));
    }

    @DeleteMapping
    @Transactional
    public ResponseEntity<ApiResponse<Void>> unregisterDeviceToken(@RequestParam String token) {
        UUID userId = SecurityUtils.getCurrentUserId();
        deviceTokenRepository.deleteByUserIdAndFcmToken(userId, token.trim());
        log.info("Unregistered FCM device token for user {}", userId);

        return ResponseEntity.ok(ApiResponse.success("Device token unregistered successfully"));
    }
}

package com.veggofresh.notification.controller;

import com.veggofresh.notification.dto.*;
import com.veggofresh.notification.entity.Notification;
import com.veggofresh.notification.service.NotificationService;
import com.veggofresh.notification.service.NotificationSenderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/notification")
@Tag(name = "Notification", description = "Notification service API")
public class NotificationController {

    private final NotificationService notificationService;
    private final NotificationSenderService notificationSenderService;

    public NotificationController(NotificationService notificationService,
                                  NotificationSenderService notificationSenderService) {
        this.notificationService = notificationService;
        this.notificationSenderService = notificationSenderService;
    }

    @PostMapping("/send")
    @Operation(summary = "Send a notification")
    @ApiResponse(responseCode = "200", description = "Notification sent successfully",
            content = @Content(schema = @Schema(implementation = NotificationResponseDto.class)))
    public ResponseEntity<NotificationResponseDto> sendNotification(
            @Valid @RequestBody NotificationSendRequestDto request) {
        Notification notification = notificationService.sendNotification(request);
        NotificationResponseDto dto = new NotificationResponseDto();
        dto.setId(notification.getId());
        dto.setTitle(notification.getTitle());
        dto.setMessage(notification.getMessage());
        dto.setStatus(notification.getStatus());
        dto.setActionUrl(notification.getActionUrl());
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get notification status by ID")
    @ApiResponse(responseCode = "200", description = "Notification status retrieved",
            content = @Content(schema = @Schema(implementation = NotificationResponseDto.class)))
    public ResponseEntity<NotificationResponseDto> getNotificationStatus(@PathVariable UUID id) {
        return notificationService.getNotificationStatus(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/count")
    @Operation(summary = "Get notification counts")
    @ApiResponse(responseCode = "200", description = "Notification counts retrieved",
            content = @Content(schema = @Schema(implementation = NotificationCountResponseDto.class)))
    public ResponseEntity<NotificationCountResponseDto> getNotificationCount(
            @RequestParam(required = false) String recipientType,
            @RequestParam(required = false) UUID recipientId) {
        NotificationCountRequestDto request = new NotificationCountRequestDto();
        request.setRecipientType(recipientType);
        request.setRecipientId(recipientId);
        return ResponseEntity.ok(notificationService.getNotificationCount(request));
    }

    @PostMapping("/register-token")
    @Operation(summary = "Register FCM device token")
    @ApiResponse(responseCode = "200", description = "Device token registered successfully")
    public ResponseEntity<Map<String, String>> registerDeviceToken(
            @Valid @RequestBody DeviceTokenRequestDto request) {
        // TODO: Persist token to user profile / delivery partner profile
        // In production: validate token, associate with user, handle platform (android/ios)
        return ResponseEntity.ok(Map.of("status", "registered", "token", request.getToken()));
    }

    @GetMapping("/list")
    @Operation(summary = "Get notifications list for a recipient")
    @ApiResponse(responseCode = "200", description = "Notifications list retrieved",
            content = @Content(schema = @Schema(implementation = NotificationResponseDto.class)))
    public ResponseEntity<NotificationListResponseDto> getNotificationsByRecipient(
            @RequestParam String recipientType,
            @RequestParam(required = false) UUID recipientId,
            Pageable pageable) {
        List<NotificationResponseDto> content = notificationService.getNotificationsByRecipient(
                recipientType, recipientId, pageable.getPageNumber(), pageable.getPageSize());

        NotificationListResponseDto dto = new NotificationListResponseDto();
        dto.setContent(content);
        dto.setSize(pageable.getPageSize());
        dto.setNumber(pageable.getPageNumber());
        // Simplified - in production would calculate totalPages from total count
        dto.setLast(true);
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/{id}/read")
    @Operation(summary = "Mark notification as read")
    @ApiResponse(responseCode = "200", description = "Notification marked as read")
    public ResponseEntity<NotificationResponseDto> markAsRead(@PathVariable UUID id) {
        return notificationService.markAsRead(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete notification")
    @ApiResponse(responseCode = "200", description = "Notification deleted")
    public ResponseEntity<NotificationResponseDto> deleteNotification(@PathVariable UUID id) {
        return notificationService.deleteNotification(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
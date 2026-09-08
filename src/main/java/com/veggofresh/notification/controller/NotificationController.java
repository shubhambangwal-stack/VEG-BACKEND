package com.veggofresh.notification.controller;

<<<<<<< HEAD
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
=======
import com.veggofresh.notification.dto.AdminBroadcastRequestDto;
import com.veggofresh.notification.dto.NotificationDto;
import com.veggofresh.notification.service.NotificationService;
import com.veggofresh.platform.common.ApiResponse;
import com.veggofresh.platform.common.PageResponse;
import com.veggofresh.platform.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

/**
 * Notification REST surface. Used by the Flutter app on launch to hydrate the
 * list and the badge count, and as the source of truth whenever the socket was
 * disconnected. The authenticated recipient (from the JWT principal) is always
 * the subject — notifications are strictly per-user.
 *
 * <p>Endpoints:
 * <ul>
 *   <li>{@code GET  /api/notifications} — paginated inbox, newest first</li>
 *   <li>{@code GET  /api/notifications/unread-count} — badge count</li>
 *   <li>{@code PUT  /api/notifications/{id}/read} — mark one read</li>
 *   <li>{@code PUT  /api/notifications/read-all} — mark all read</li>
 *   <li>{@code POST /api/notifications/broadcast} — ADMIN-only announcement
 *       (all roles or filtered by role)</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<NotificationDto>>> getNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID recipientId = SecurityUtils.getCurrentUserId();
        Page<NotificationDto> result = notificationService.getNotifications(recipientId, PageRequest.of(page, Math.min(size, 100)));
        return ResponseEntity.ok(ApiResponse.success(PageResponse.of(result), "Notifications retrieved successfully"));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getUnreadCount() {
        UUID recipientId = SecurityUtils.getCurrentUserId();
        long count = notificationService.getUnreadCount(recipientId);
        return ResponseEntity.ok(ApiResponse.success(Map.of("unreadCount", count), "Unread count retrieved successfully"));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(@PathVariable UUID id) {
        notificationService.markAsRead(SecurityUtils.getCurrentUserId(), id);
        return ResponseEntity.ok(ApiResponse.success("Notification marked as read"));
    }

    @PutMapping("/read-all")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> markAllAsRead() {
        UUID recipientId = SecurityUtils.getCurrentUserId();
        int updated = notificationService.markAllAsRead(recipientId);
        return ResponseEntity.ok(ApiResponse.success(Map.of("updated", updated), "All notifications marked as read"));
    }

    @PostMapping("/broadcast")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> broadcast(
            @Valid @RequestBody AdminBroadcastRequestDto request) {
        int recipients = notificationService.broadcast(request.getTitle(), request.getBody(),
                request.getData(), request.getRecipientRole());
        return ResponseEntity.ok(ApiResponse.success(Map.of("recipients", recipients), "Announcement sent successfully"));
>>>>>>> 5d59f32924e5d18dc9e8d7fe3f7ff5cb7a78a1a2
    }
}
package com.veggofresh.notification.controller;

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
    }
}
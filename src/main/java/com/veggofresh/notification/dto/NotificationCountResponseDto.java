package com.veggofresh.notification.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationCountResponseDto {
    private Long pendingCount;
    private Long sentCount;
    private Long readCount;
    private Long failedCount;
    private Long expiredCount;
}
package com.veggofresh.vendor.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class ApplicationStatusResponseDto {
    private String status; // pending, approved, declined, under_review
    private String declineReason;
    private Instant submittedAt;
}

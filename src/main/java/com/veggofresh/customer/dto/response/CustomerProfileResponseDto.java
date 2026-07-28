package com.veggofresh.customer.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerProfileResponseDto {
    private UUID id;
    private UUID userId;
    private String fullName;
    private String avatarUrl;
    private String phone;
    private String email;
    private int memberSinceYear;   // derived from createdAt.atZone(UTC).getYear()
    private Instant createdAt;
    private Instant updatedAt;
}

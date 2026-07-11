package com.veggofresh.auth.dto.response;

import com.veggofresh.auth.entity.UserRole;
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
public class UserProfileResponseDto {
    private UUID id;
    private String phone;
    private String email;
    private UserRole role;
    private boolean isVerified;
    private boolean isBlocked;
    private Instant createdAt;
}

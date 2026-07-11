package com.veggofresh.auth.dto;

import com.veggofresh.auth.entity.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSummaryDto {
    private UUID id;
    private String phone;
    private String email;
    private UserRole role;
    private boolean isVerified;
    private boolean isBlocked;
}

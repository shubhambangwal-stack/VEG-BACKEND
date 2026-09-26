package com.veggofresh.auth.dto.request;

import com.veggofresh.auth.entity.UserRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class FirebaseOtpVerifyDto {
    @NotBlank(message = "Firebase ID token is required")
    private String idToken;

    @NotNull(message = "Role is required")
    private UserRole role;
}

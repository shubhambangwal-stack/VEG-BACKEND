package com.veggofresh.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LogoutRequestDto {
    @NotBlank(message = "Refresh token is required")
    private String refreshToken;
}

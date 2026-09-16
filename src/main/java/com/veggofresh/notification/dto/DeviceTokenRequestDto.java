package com.veggofresh.notification.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceTokenRequestDto {

    @NotBlank(message = "token is required")
    private String token;

    private String platform; // ANDROID, IOS, WEB

    private UUID userId;
}
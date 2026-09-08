package com.veggofresh.notification.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceTokenRequestDto {
    private String token;
    private String platform; // ANDROID, IOS, WEB
    private UUID userId;
}
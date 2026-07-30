package com.veggofresh.delivery.dto.response;

import com.veggofresh.delivery.entity.DeliveryKycStatus;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class DeliveryProfileResponseDto {
    private UUID id;
    private UUID userId;
    private String phone;
    private DeliveryKycStatus kycStatus;
    private boolean online;
    private Double currentLatitude;
    private Double currentLongitude;
    private String vehicleType;
}

package com.veggofresh.vendor.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class ShopDto {
    private UUID id;
    private UUID ownerUserId;
    private String name;
    private String address;
    private Double latitude;
    private Double longitude;
    private String kycStatus;
    private boolean isOnline;
}

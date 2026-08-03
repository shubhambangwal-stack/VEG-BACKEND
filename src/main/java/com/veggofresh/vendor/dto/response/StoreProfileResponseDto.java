package com.veggofresh.vendor.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class StoreProfileResponseDto {
    private UUID id;
    private String storeName;
    private String storeBio;
    private String storeImageUrl;
    private List<String> attributes;
    private String streetAddress;
    private String city;
    private String zipCode;
    private Double latitude;
    private Double longitude;
    private boolean isOnline;
}

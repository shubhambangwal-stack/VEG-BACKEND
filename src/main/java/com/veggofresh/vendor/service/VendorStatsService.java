package com.veggofresh.vendor.service;

import com.veggofresh.vendor.dto.response.VendorProfileStatsResponseDto;

import java.util.UUID;

public interface VendorStatsService {
    VendorProfileStatsResponseDto getProfileStats(UUID ownerUserId);
}

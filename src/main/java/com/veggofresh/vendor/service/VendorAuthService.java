 package com.veggofresh.vendor.service;

import com.veggofresh.vendor.dto.request.VendorLoginRequestDto;
import com.veggofresh.vendor.dto.request.VendorRegisterRequestDto;
import com.veggofresh.vendor.dto.response.VendorAuthResponseDto;

public interface VendorAuthService {
    VendorAuthResponseDto login(VendorLoginRequestDto request);
    VendorAuthResponseDto register(VendorRegisterRequestDto request);
}

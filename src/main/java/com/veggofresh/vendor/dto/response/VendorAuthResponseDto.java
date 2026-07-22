package com.veggofresh.vendor.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VendorAuthResponseDto {
    private String token;
    private VendorProfileDto vendor;
}

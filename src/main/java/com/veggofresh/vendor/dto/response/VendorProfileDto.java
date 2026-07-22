package com.veggofresh.vendor.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VendorProfileDto {
    private String id;
    private String fullName;
    private String businessName;
    private String email;
    private String phone;
    private String businessType;
}

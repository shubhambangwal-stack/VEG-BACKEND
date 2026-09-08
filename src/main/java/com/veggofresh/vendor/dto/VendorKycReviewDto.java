package com.veggofresh.vendor.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/** Cross-module DTO -- consumed by Admin's AdminVendorKycController. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VendorKycReviewDto {
    private UUID shopId;
    private UUID ownerUserId;
    private String businessName;
    private String ownerFullName;
    private String businessPhone;
    private String email;
    private String businessType;
    private String kycStatus;
    private String rejectionReason;
    private Instant submittedAt;
}

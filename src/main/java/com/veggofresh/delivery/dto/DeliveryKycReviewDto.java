package com.veggofresh.delivery.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/** Cross-module DTO -- consumed by Admin's AdminDeliveryKycController. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryKycReviewDto {
    private UUID userId;
    private String fullName;
    private String phone;
    private String vehicleType;
    private String vehicleModel;
    private String plateNumber;
    private String licenseNumber;
    private String kycStatus;
    private String rejectionReason;
    private Instant submittedAt;
}

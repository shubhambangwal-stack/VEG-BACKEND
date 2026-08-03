package com.veggofresh.vendor.dto.response;

import com.veggofresh.vendor.entity.KycStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class VendorOnboardingStatusResponseDto {
    private boolean hasBasicInfo;
    private boolean hasBusinessLocation;
    private boolean documentsSubmitted;
    private KycStatus kycStatus;
    private String rejectionReason;
    private Instant applicationSubmittedAt;
    private VendorOnboardingNextAction nextAction;
}

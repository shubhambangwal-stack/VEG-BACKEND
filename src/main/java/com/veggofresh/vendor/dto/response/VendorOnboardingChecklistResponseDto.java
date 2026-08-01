package com.veggofresh.vendor.dto.response;

import lombok.Builder;
import lombok.Getter;

/** Figma "Application Accepted" post-approval checklist. */
@Getter
@Builder
public class VendorOnboardingChecklistResponseDto {
    private boolean hasFirstProduct;
    private boolean hasDeliveryRange;
    private boolean hasPaymentSettings;
    private boolean allComplete;
}

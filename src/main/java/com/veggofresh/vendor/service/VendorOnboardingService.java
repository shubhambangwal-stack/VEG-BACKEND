package com.veggofresh.vendor.service;

import com.veggofresh.vendor.dto.request.VendorBasicInfoRequestDto;
import com.veggofresh.vendor.dto.request.VendorBusinessLocationRequestDto;
import com.veggofresh.vendor.dto.response.VendorOnboardingChecklistResponseDto;
import com.veggofresh.vendor.dto.response.VendorOnboardingStatusResponseDto;

import java.util.UUID;

public interface VendorOnboardingService {
    /** Call right after OTP login succeeds -- tells the app exactly which screen to show next. */
    VendorOnboardingStatusResponseDto getStatus(UUID ownerUserId);

    VendorOnboardingStatusResponseDto submitBasicInfo(UUID ownerUserId, VendorBasicInfoRequestDto request);
    VendorOnboardingStatusResponseDto submitBusinessLocation(UUID ownerUserId, VendorBusinessLocationRequestDto request);

    /** Requires all 3 documents already uploaded via VendorDocumentService. Sets kycStatus PENDING. */
    VendorOnboardingStatusResponseDto submitApplication(UUID ownerUserId);

    /** Post-approval "getting started" checklist -- only meaningful once kycStatus is APPROVED. */
    VendorOnboardingChecklistResponseDto getChecklist(UUID ownerUserId);
}

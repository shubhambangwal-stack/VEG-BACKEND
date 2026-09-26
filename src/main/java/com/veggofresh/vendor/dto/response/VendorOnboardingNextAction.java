package com.veggofresh.vendor.dto.response;

/** Tells the frontend exactly which onboarding screen to route to next. */
public enum VendorOnboardingNextAction {
    BASIC_INFO,
    BUSINESS_LOCATION,
    BANK_DETAILS,
    VERIFICATION_DOCUMENTS,
    UNDER_REVIEW,
    REJECTED,
    DASHBOARD
}

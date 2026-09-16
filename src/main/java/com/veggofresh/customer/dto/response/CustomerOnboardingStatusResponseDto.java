package com.veggofresh.customer.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * Mirrors Delivery/Vendor's OnboardingStatusResponseDto pattern. Customer has
 * no KYC, so this is just the one basic-info flag plus the routing hint --
 * client should call this once right after OTP verification (and again on
 * app relaunch) instead of inferring "first time" from GET /profile's
 * fullName being blank.
 */
@Getter
@Builder
public class CustomerOnboardingStatusResponseDto {
    private boolean hasBasicInfo;
    private CustomerOnboardingNextAction nextAction;
}

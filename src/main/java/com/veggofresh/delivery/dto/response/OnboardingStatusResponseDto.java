package com.veggofresh.delivery.dto.response;

import com.veggofresh.delivery.entity.DeliveryKycStatus;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OnboardingStatusResponseDto {
    private boolean hasBasicInfo;
    private int verificationStep;
    private boolean isSubmitted;
    private DeliveryKycStatus kycStatus;
    private OnboardingNextAction nextAction;
}

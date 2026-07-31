package com.veggofresh.delivery.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class ProofOfDeliveryResponseDto {
    private String photoUrl;
    private boolean deliveredToCustomerDirectly;
    private boolean leftAtFrontDoor;
    private boolean packagingIntact;
    private boolean addressVerifiedManually;
    private String notes;
    private Instant submittedAt;
}

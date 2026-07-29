package com.veggofresh.delivery.dto.response;

import com.veggofresh.delivery.entity.DeliveryAssignmentStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class DeliveryAssignmentResponseDto {
    private UUID id;
    private UUID orderId;
    private DeliveryAssignmentStatus status;
    private double pickupLatitude;
    private double pickupLongitude;
    private double dropLatitude;
    private double dropLongitude;
    private Instant assignedAt;
    private Instant expiresAt;

    // Phase B: contact info + timeline. shopPhone/customerPhone resolved live via
    // UserLookupService; shopName/shopAddress are the dispatch-time snapshot (see
    // NOTES.md -- no ShopLookupService exists yet). All nullable: populated only
    // on the single-assignment detail endpoint, not on list/nearby endpoints.
    private String shopName;
    private String shopAddress;
    private String shopPhone;
    private String customerPhone;
    private List<TimelineEntryDto> timeline;

    @Getter
    @Builder
    public static class TimelineEntryDto {
        private DeliveryAssignmentStatus status;
        private Instant occurredAt;
    }
}

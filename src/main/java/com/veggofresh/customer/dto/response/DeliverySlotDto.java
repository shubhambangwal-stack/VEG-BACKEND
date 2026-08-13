package com.veggofresh.customer.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliverySlotDto {
    private UUID slotId;
    private String date;
    private String label;
    private String startTime;
    private String endTime;
    private boolean isAvailable;
}

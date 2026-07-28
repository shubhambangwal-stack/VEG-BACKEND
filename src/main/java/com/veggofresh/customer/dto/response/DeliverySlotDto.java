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
    private String date;       // "2024-10-24"
    private String label;      // "09:00 - 11:00"
    private String startTime;  // "09:00"
    private String endTime;    // "11:00"
    private boolean isAvailable;
}

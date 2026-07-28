package com.veggofresh.customer.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatusTimelineDto {
    /** 1=Placed, 2=Preparing, 3=On the way, 4=Delivered */
    private int step;
    /** Human-readable step label: "Placed", "Preparing", "On the way", "Delivered" */
    private String label;
    /** Null if step not yet reached */
    private Instant completedAt;
    private boolean isCurrent;
}

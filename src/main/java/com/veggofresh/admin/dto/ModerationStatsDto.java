package com.veggofresh.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Stats for the "Moderation Center" dashboard header cards (screen 3 of admin images).
 * Cards: Unrestricted Users, Suspended/Blocked, Flagged Warnings
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModerationStatsDto {

    /** UNRESTRICTED USERS — accounts with no flags and isBlocked=false */
    private long unrestrictedUsers;

    /** SUSPENDED / BLOCKED — accounts where isBlocked=true */
    private long suspendedOrBlocked;

    /** FLAGGED WARNINGS — accounts with flagCount > 0 but not yet blocked */
    private long flaggedWarnings;
}

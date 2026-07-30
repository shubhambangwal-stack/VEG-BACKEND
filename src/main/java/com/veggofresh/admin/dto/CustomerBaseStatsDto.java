package com.veggofresh.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Stats for the "Customer Base" dashboard header cards (screen 1 of admin images).
 * Cards: Total Customers, Active This Month, Premium Subscribers, Suspended Accounts
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerBaseStatsDto {

    /** TOTAL CUSTOMERS card */
    private long totalCustomers;

    /**
     * ACTIVE THIS MONTH card —
     * customers who placed at least one order in the current calendar month
     */
    private long activeThisMonth;

    /**
     * PREMIUM SUBSCRIBERS card —
     * customers with lifetime outlay >= premium threshold (>=5000 INR)
     */
    private long premiumSubscribers;

    /**
     * SUSPENDED ACCOUNTS card — count of is_blocked=true customers
     */
    private long suspendedAccounts;
}

package com.veggofresh.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Customer row in the "Registered Customer Ledger" table shown in the admin dashboard.
 * Columns: Customer ID, Profile Details, Completed Purchases, Lifetime Outlay,
 *          Registration Date, Account State, Actions
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminCustomerLedgerDto {

    /** e.g. "CUST-84920" — human-readable customer identifier */
    private String customerId;

    private UUID userId;

    /** Display name */
    private String fullName;

    /** Email shown under the name in the ledger */
    private String email;

    private String phone;

    /** Avatar URL for profile picture */
    private String avatarUrl;

    /** Total number of DELIVERED orders */
    private long completedPurchases;

    /** Sum of all order totalAmount (lifetime spend) */
    private BigDecimal lifetimeOutlay;

    /** Timestamp when the user registered */
    private Instant registrationDate;

    /**
     * Account state label shown in UI.
     * Values: "Active", "Suspended", "Inactive"
     */
    private String accountState;

    private boolean isBlocked;

    private boolean isVerified;
}

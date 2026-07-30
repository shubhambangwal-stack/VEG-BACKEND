package com.veggofresh.admin.service;

import com.veggofresh.admin.dto.AdminCustomerLedgerDto;
import com.veggofresh.admin.dto.CustomerBaseStatsDto;
import com.veggofresh.admin.dto.ModerationLedgerRowDto;
import com.veggofresh.admin.dto.ModerationStatsDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * Admin module service for customer management.
 * Serves the "Customer Management" section of the admin panel:
 *  - Customer Base dashboard (View Customers)
 *  - Moderation Center (Block / Unblock Accounts)
 */
public interface AdminCustomerService {

    // ── Customer Base (View Customers screen) ───────────────────────────────

    /** 4 header stat cards on the Customer Base page */
    CustomerBaseStatsDto getCustomerBaseStats();

    /**
     * Registered Customer Ledger — paginated list with optional search.
     * @param search optional search string matched against name, email, or CUST-ID
     */
    Page<AdminCustomerLedgerDto> getCustomerLedger(String search, Pageable pageable);

    /** Single customer detail view (eye icon action) */
    AdminCustomerLedgerDto getCustomerById(UUID userId);

    /** Soft-delete a customer account (delete icon action) */
    void deleteCustomer(UUID userId);

    // ── Moderation Center (Block / Unblock Accounts screen) ─────────────────

    /** 3 header stat cards on the Moderation Center page */
    ModerationStatsDto getModerationStats();

    /**
     * User Moderation Ledger — paginated list with flags and infractions.
     * @param search optional search string
     */
    Page<ModerationLedgerRowDto> getModerationLedger(String search, Pageable pageable);

    /** Block a customer account (Block Account action) */
    void blockCustomer(UUID userId);

    /** Unblock/restore a customer account (Restore Access action) */
    void unblockCustomer(UUID userId);
}

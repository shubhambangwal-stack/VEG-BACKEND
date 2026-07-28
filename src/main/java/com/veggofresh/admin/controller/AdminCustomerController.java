package com.veggofresh.admin.controller;

import com.veggofresh.admin.dto.AdminCustomerLedgerDto;
import com.veggofresh.admin.dto.CustomerBaseStatsDto;
import com.veggofresh.admin.dto.ModerationLedgerRowDto;
import com.veggofresh.admin.dto.ModerationStatsDto;
import com.veggofresh.admin.service.AdminCustomerService;
import com.veggofresh.platform.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Admin Customer Management Controller
 *
 * <pre>
 * ── Customer Base (View Customers screen) ──────────────────────────────────
 * GET  /api/admin/customers/stats               → 4 header stat cards
 * GET  /api/admin/customers                     → Registered Customer Ledger (paginated + search)
 * GET  /api/admin/customers/{userId}            → Single customer detail (eye icon)
 * DELETE /api/admin/customers/{userId}          → Soft-delete customer (delete icon)
 *
 * ── Moderation Center (Block / Unblock Accounts screen) ───────────────────
 * GET  /api/admin/customers/moderation/stats    → 3 moderation stat cards
 * GET  /api/admin/customers/moderation          → User Moderation Ledger (paginated + search)
 * PUT  /api/admin/customers/{userId}/block      → Block Account action
 * PUT  /api/admin/customers/{userId}/unblock    → Restore Access action
 * </pre>
 */
@RestController
@RequestMapping("/api/admin/customers")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminCustomerController {

    private final AdminCustomerService adminCustomerService;

    // ─────────────────────────────────────────────────────────────────────────
    // CUSTOMER BASE — View Customers screen
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * GET /api/admin/customers/stats
     * Returns the 4 stat cards: Total Customers, Active This Month,
     * Premium Subscribers, Suspended Accounts.
     */
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<CustomerBaseStatsDto>> getCustomerBaseStats() {
        CustomerBaseStatsDto stats = adminCustomerService.getCustomerBaseStats();
        return ResponseEntity.ok(ApiResponse.success(stats, "Customer base stats retrieved successfully"));
    }

    /**
     * GET /api/admin/customers?search=&page=0&size=20
     * Registered Customer Ledger — paginated + searchable by ID, name, email.
     * Columns: Customer ID, Profile Details, Completed Purchases,
     *          Lifetime Outlay, Registration Date, Account State
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<AdminCustomerLedgerDto>>> getCustomerLedger(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<AdminCustomerLedgerDto> ledger = adminCustomerService.getCustomerLedger(search, pageable);
        return ResponseEntity.ok(ApiResponse.success(ledger, "Customer ledger retrieved successfully"));
    }

    /**
     * GET /api/admin/customers/{userId}
     * Single customer detail — triggered by the eye (view) icon in the ledger.
     */
    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<AdminCustomerLedgerDto>> getCustomerById(@PathVariable UUID userId) {
        AdminCustomerLedgerDto customer = adminCustomerService.getCustomerById(userId);
        return ResponseEntity.ok(ApiResponse.success(customer, "Customer details retrieved successfully"));
    }

    /**
     * DELETE /api/admin/customers/{userId}
     * Soft-deletes a customer account — triggered by the delete (trash) icon.
     */
    @DeleteMapping("/{userId}")
    public ResponseEntity<ApiResponse<Void>> deleteCustomer(@PathVariable UUID userId) {
        adminCustomerService.deleteCustomer(userId);
        return ResponseEntity.ok(ApiResponse.success("Customer account deleted successfully"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MODERATION CENTER — Block / Unblock Accounts screen
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * GET /api/admin/customers/moderation/stats
     * Returns 3 moderation cards: Unrestricted Users, Suspended/Blocked, Flagged Warnings.
     */
    @GetMapping("/moderation/stats")
    public ResponseEntity<ApiResponse<ModerationStatsDto>> getModerationStats() {
        ModerationStatsDto stats = adminCustomerService.getModerationStats();
        return ResponseEntity.ok(ApiResponse.success(stats, "Moderation stats retrieved successfully"));
    }

    /**
     * GET /api/admin/customers/moderation?search=&page=0&size=20
     * User Moderation Ledger — paginated + searchable.
     * Columns: Customer Details, Account ID, Flags Raised, Primary Infraction,
     *          Account State, Moderation Actions
     */
    @GetMapping("/moderation")
    public ResponseEntity<ApiResponse<Page<ModerationLedgerRowDto>>> getModerationLedger(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<ModerationLedgerRowDto> ledger = adminCustomerService.getModerationLedger(search, pageable);
        return ResponseEntity.ok(ApiResponse.success(ledger, "Moderation ledger retrieved successfully"));
    }

    /**
     * PUT /api/admin/customers/{userId}/block
     * Blocks a customer account — "Block Account" button in the moderation ledger.
     */
    @PutMapping("/{userId}/block")
    public ResponseEntity<ApiResponse<Void>> blockCustomer(@PathVariable UUID userId) {
        adminCustomerService.blockCustomer(userId);
        return ResponseEntity.ok(ApiResponse.success("Customer account blocked successfully"));
    }

    /**
     * PUT /api/admin/customers/{userId}/unblock
     * Restores customer access — "Restore Access" button in the moderation ledger.
     */
    @PutMapping("/{userId}/unblock")
    public ResponseEntity<ApiResponse<Void>> unblockCustomer(@PathVariable UUID userId) {
        adminCustomerService.unblockCustomer(userId);
        return ResponseEntity.ok(ApiResponse.success("Customer access restored successfully"));
    }
}

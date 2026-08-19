package com.veggofresh.payment.controller;

import com.veggofresh.payment.dto.request.WithdrawRequest;
import com.veggofresh.payment.dto.response.WalletResponse;
import com.veggofresh.payment.dto.response.WalletTransactionResponse;
import com.veggofresh.payment.entity.Wallet;
import com.veggofresh.payment.entity.WalletTransaction;
import com.veggofresh.payment.entity.WalletTransactionType;
import com.veggofresh.payment.service.WalletService;
import com.veggofresh.platform.common.ApiResponse;
import com.veggofresh.platform.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Wallet Controller � same endpoints serve all roles (Customer, Vendor, Delivery, Admin).
 *
 * <pre>
 * GET  /api/payment/wallet                          ? Get balance
 * GET  /api/payment/wallet/transactions             ? Full history
 * GET  /api/payment/wallet/transactions?type=CREDIT ? Credits only
 * GET  /api/payment/wallet/transactions?type=DEBIT  ? Debits only
 * POST /api/payment/wallet/withdraw                 ? Request withdrawal (STUBBED)
 * </pre>
 */
@RestController
@RequestMapping("/api/payment/wallet")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    /** Returns the caller''s wallet balance. Auto-creates the wallet if first access. */
    @GetMapping
    public ResponseEntity<ApiResponse<WalletResponse>> getWallet() {
        UUID userId = SecurityUtils.getCurrentUserId();
        String role = resolveRole();
        Wallet wallet = walletService.getOrCreateWallet(userId, role);
        return ResponseEntity.ok(ApiResponse.success(toWalletResponse(wallet), "Wallet retrieved"));
    }

    /**
     * Returns transaction history.
     * @param type optional filter: CREDIT | DEBIT | DEBIT_RESERVED | RESERVATION_RELEASED | WITHDRAWAL
     */
    @GetMapping("/transactions")
    public ResponseEntity<ApiResponse<List<WalletTransactionResponse>>> getTransactions(
            @RequestParam(required = false) WalletTransactionType type) {
        UUID userId = SecurityUtils.getCurrentUserId();
        List<WalletTransaction> txns = type != null
                ? walletService.getTransactionsByType(userId, type)
                : walletService.getTransactions(userId);
        List<WalletTransactionResponse> response = txns.stream()
                .map(this::toTransactionResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(response, "Transactions retrieved"));
    }

    /**
     * Initiates a withdrawal. Currently STUBBED � deducts balance in ledger only.
     * Real Razorpay Route payout will be wired once business KYC is approved.
     */
    @PostMapping("/withdraw")
    public ResponseEntity<ApiResponse<String>> withdraw(@Valid @RequestBody WithdrawRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        String desc = request.getNote() != null ? request.getNote() : "Withdrawal request";
        walletService.withdraw(userId, request.getAmount(), desc);
        return ResponseEntity.ok(ApiResponse.success(
                "Withdrawal of \u20B9" + request.getAmount() + " initiated successfully",
                "Withdrawal processed (bank transfer will be processed within 2-3 business days)"));
    }

    // -- Helpers -----------------------------------------------

    private String resolveRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getAuthorities() == null) return "CUSTOMER";
        return auth.getAuthorities().stream()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .findFirst()
                .orElse("CUSTOMER");
    }

    private WalletResponse toWalletResponse(Wallet w) {
        return WalletResponse.builder()
                .walletId(w.getId())
                .userId(w.getUserId())
                .role(w.getRole())
                .availableBalance(w.getAvailableBalance())
                .reservedBalance(w.getReservedBalance())
                .totalBalance(w.getAvailableBalance().add(w.getReservedBalance()))
                .build();
    }

    private WalletTransactionResponse toTransactionResponse(WalletTransaction t) {
        return WalletTransactionResponse.builder()
                .id(t.getId())
                .orderId(t.getOrderId())
                .razorpayPaymentId(t.getRazorpayPaymentId())
                .type(t.getType())
                .amount(t.getAmount())
                .description(t.getDescription())
                .createdAt(t.getCreatedAt())
                .build();
    }
}

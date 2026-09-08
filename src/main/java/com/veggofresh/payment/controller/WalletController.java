package com.veggofresh.payment.controller;

import com.veggofresh.payment.dto.WalletBalanceDto;
import com.veggofresh.payment.dto.WalletTransactionDto;
import com.veggofresh.payment.service.WalletService;
import com.veggofresh.platform.common.ApiResponse;
import com.veggofresh.platform.common.PageResponse;
import com.veggofresh.platform.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Deliberately role-agnostic -- Customer, Vendor, and Delivery all have exactly the
 * same wallet shape, so one endpoint serves all three (resolves whoever the bearer
 * token belongs to via SecurityUtils, same as every other authenticated controller in
 * this codebase). No role check beyond "authenticated" -- there's nothing role-specific
 * to gate here.
 *
 * Read-only this round: no credit/debit HTTP endpoints. Credits/debits only happen as
 * a side effect of real domain actions (e.g. Customer's OrderServiceImpl.cancelOrder())
 * calling WalletService directly -- never as a standalone "add money to my wallet"
 * customer-facing action, which would be a real payment-collection feature belonging
 * to the full checkout-wallet design, not this narrow round.
 */
@RestController
@RequestMapping("/api/payment/wallet")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    @GetMapping("/balance")
    public ResponseEntity<ApiResponse<WalletBalanceDto>> getBalance() {
        WalletBalanceDto balance = walletService.getBalance(SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success(balance, "Wallet balance retrieved successfully"));
    }

    @GetMapping("/transactions")
    public ResponseEntity<ApiResponse<PageResponse<WalletTransactionDto>>> getTransactionHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<WalletTransactionDto> result = walletService.getTransactionHistory(
                SecurityUtils.getCurrentUserId(), PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(PageResponse.of(result), "Wallet transaction history retrieved successfully"));
    }
}

package com.veggofresh.payment.service;

import com.veggofresh.payment.dto.WalletBalanceDto;
import com.veggofresh.payment.dto.WalletTransactionDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Cross-module interface -- Customer, Vendor, and Delivery all read/write wallets
 * through this, never by importing Wallet/WalletTransaction @Entity directly.
 *
 * NARROW SCOPE (this round, per PROJECT_STATE section 6 / conversation): only what
 * cancellation-refund needs. credit()/debit() are immediate, real ledger movements --
 * NOT the soft-reservation (hold now, finalize-or-release later) semantics the full
 * checkout-wallet design needs. That needs real Payment/Razorpay integration to exist
 * first and is intentionally not built yet. This interface is the real foundation that
 * design will extend, not a stub that gets replaced.
 */
public interface WalletService {

    /** Auto-creates a zero-balance wallet on first access, same getOrCreate pattern used everywhere else in this codebase. */
    WalletBalanceDto getBalance(UUID userId);

    /** Immediate, real balance increase. Amount must be positive. */
    WalletTransactionDto credit(UUID userId, BigDecimal amount, WalletTransactionReason reason,
                                 UUID referenceId, String description);

    /** Immediate, real balance decrease. Amount must be positive and <= current balance -- throws INSUFFICIENT_WALLET_BALANCE otherwise. */
    WalletTransactionDto debit(UUID userId, BigDecimal amount, WalletTransactionReason reason,
                                UUID referenceId, String description);

    Page<WalletTransactionDto> getTransactionHistory(UUID userId, Pageable pageable);
}

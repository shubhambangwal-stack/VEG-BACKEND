package com.veggofresh.payment.service;

import com.veggofresh.payment.entity.Wallet;
import com.veggofresh.payment.entity.WalletTransaction;
import com.veggofresh.payment.entity.WalletTransactionType;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Unified Wallet service interface � used by Payment, Customer, Vendor, Delivery, Admin modules.
 *
 * <p>Every method is transactional and uses optimistic locking (@Version on BaseEntity)
 * to prevent concurrent balance corruption.
 */
public interface WalletService {

    /**
     * Returns the wallet for the given user, creating it if it doesn''t exist yet.
     *
     * @param userId the user whose wallet to fetch
     * @param role   CUSTOMER | VENDOR | DELIVERY | ADMIN (used only on creation)
     */
    Wallet getOrCreateWallet(UUID userId, String role);

    /**
     * Credits money to the user''s available balance.
     * Used for: earnings settlement, refunds.
     */
    void credit(UUID userId, String role, BigDecimal amount, UUID orderId, String description);

    /**
     * Permanently debits the user''s available balance.
     * Used for: wallet-funded order payment finalization on vendor accept.
     */
    void debit(UUID userId, BigDecimal amount, UUID orderId, String description);

    /**
     * Soft-holds (reserves) money from available ? reserved balance.
     * Used at checkout time, mirroring Razorpay''s authorize/hold.
     *
     * @throws com.veggofresh.platform.exception.BusinessException if insufficient available balance
     */
    void reserve(UUID userId, BigDecimal amount, UUID orderId, String description);

    /**
     * Releases a reservation back to available balance.
     * Called when: vendor-accept timeout expires, order cancelled before vendor accept.
     */
    void releaseReservation(UUID userId, BigDecimal amount, UUID orderId, String description);

    /**
     * Finalizes a reservation into a permanent debit.
     * Called when: vendor accepts the order (alongside Razorpay capture).
     */
    void finalizeReservation(UUID userId, BigDecimal amount, UUID orderId, String razorpayPaymentId);

    /**
     * Initiates a withdrawal from available balance.
     * The Razorpay Route call is STUBBED � it only deducts the ledger balance for now.
     *
     * @throws com.veggofresh.platform.exception.BusinessException if insufficient available balance
     */
    void withdraw(UUID userId, BigDecimal amount, String description);

    /** Returns full transaction history for a wallet (newest first). */
    List<WalletTransaction> getTransactions(UUID userId);

    /** Returns filtered transaction history by type (CREDIT or DEBIT). */
    List<WalletTransaction> getTransactionsByType(UUID userId, WalletTransactionType type);
}

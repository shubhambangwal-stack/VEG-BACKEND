package com.veggofresh.payment.service.impl;

import com.veggofresh.payment.entity.Wallet;
import com.veggofresh.payment.entity.WalletTransaction;
import com.veggofresh.payment.entity.WalletTransactionType;
import com.veggofresh.payment.repository.WalletRepository;
import com.veggofresh.payment.repository.WalletTransactionRepository;
import com.veggofresh.payment.service.WalletService;
import com.veggofresh.platform.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class WalletServiceImpl implements WalletService {

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;

    // -- getOrCreateWallet -------------------------------------

    @Override
    public Wallet getOrCreateWallet(UUID userId, String role) {
        return walletRepository.findByUserId(userId).orElseGet(() -> {
            Wallet wallet = new Wallet();
            wallet.setUserId(userId);
            wallet.setRole(role);
            wallet.setAvailableBalance(BigDecimal.ZERO);
            wallet.setReservedBalance(BigDecimal.ZERO);
            log.info("Auto-creating wallet for userId={}, role={}", userId, role);
            return walletRepository.save(wallet);
        });
    }

    // -- credit ------------------------------------------------

    @Override
    public void credit(UUID userId, String role, BigDecimal amount, UUID orderId, String description) {
        Wallet wallet = getOrCreateWallet(userId, role);
        wallet.setAvailableBalance(wallet.getAvailableBalance().add(amount));
        walletRepository.save(wallet);
        recordTransaction(wallet.getId(), orderId, null, WalletTransactionType.CREDIT, amount, description);
        log.info("Wallet CREDIT: userId={}, amount={}, desc={}", userId, amount, description);
    }

    // -- debit -------------------------------------------------

    @Override
    public void debit(UUID userId, BigDecimal amount, UUID orderId, String description) {
        Wallet wallet = getOrRequireWallet(userId);
        assertSufficientAvailable(wallet, amount);
        wallet.setAvailableBalance(wallet.getAvailableBalance().subtract(amount));
        walletRepository.save(wallet);
        recordTransaction(wallet.getId(), orderId, null, WalletTransactionType.DEBIT, amount, description);
        log.info("Wallet DEBIT: userId={}, amount={}, desc={}", userId, amount, description);
    }

    // -- reserve -----------------------------------------------

    @Override
    public void reserve(UUID userId, BigDecimal amount, UUID orderId, String description) {
        Wallet wallet = getOrRequireWallet(userId);
        assertSufficientAvailable(wallet, amount);
        wallet.setAvailableBalance(wallet.getAvailableBalance().subtract(amount));
        wallet.setReservedBalance(wallet.getReservedBalance().add(amount));
        walletRepository.save(wallet);
        recordTransaction(wallet.getId(), orderId, null, WalletTransactionType.DEBIT_RESERVED, amount, description);
        log.info("Wallet RESERVE: userId={}, amount={}, orderId={}", userId, amount, orderId);
    }

    // -- releaseReservation ------------------------------------

    @Override
    public void releaseReservation(UUID userId, BigDecimal amount, UUID orderId, String description) {
        Wallet wallet = getOrRequireWallet(userId);
        wallet.setReservedBalance(wallet.getReservedBalance().subtract(amount));
        wallet.setAvailableBalance(wallet.getAvailableBalance().add(amount));
        walletRepository.save(wallet);
        recordTransaction(wallet.getId(), orderId, null, WalletTransactionType.RESERVATION_RELEASED, amount, description);
        log.info("Wallet RELEASE RESERVATION: userId={}, amount={}, orderId={}", userId, amount, orderId);
    }

    // -- finalizeReservation -----------------------------------

    @Override
    public void finalizeReservation(UUID userId, BigDecimal amount, UUID orderId, String razorpayPaymentId) {
        Wallet wallet = getOrRequireWallet(userId);
        // Move from reserved ? permanently gone (debited)
        wallet.setReservedBalance(wallet.getReservedBalance().subtract(amount));
        walletRepository.save(wallet);
        recordTransaction(wallet.getId(), orderId, razorpayPaymentId, WalletTransactionType.DEBIT,
                amount, "Wallet payment finalized for order #" + orderId);
        log.info("Wallet FINALIZE RESERVATION: userId={}, amount={}, orderId={}", userId, amount, orderId);
    }

    // -- withdraw ----------------------------------------------

    @Override
    public void withdraw(UUID userId, BigDecimal amount, String description) {
        Wallet wallet = getOrRequireWallet(userId);
        assertSufficientAvailable(wallet, amount);
        wallet.setAvailableBalance(wallet.getAvailableBalance().subtract(amount));
        walletRepository.save(wallet);
        recordTransaction(wallet.getId(), null, null, WalletTransactionType.WITHDRAWAL, amount, description);

        // -- STUBBED: Razorpay Route Payout API call goes here --
        // Once business KYC is approved and Route is activated, replace this log
        // with a real RazorpayX Payout API call:
        //   razorpayXClient.payouts.create(payoutRequest)
        log.info("[STUBBED] Payout of ?{} initiated for userId={}. Ledger debited. No real bank transfer yet.", amount, userId);
    }

    // -- read --------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public List<WalletTransaction> getTransactions(UUID userId) {
        Wallet wallet = getOrRequireWallet(userId);
        return walletTransactionRepository.findByWalletIdOrderByCreatedAtDesc(wallet.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<WalletTransaction> getTransactionsByType(UUID userId, WalletTransactionType type) {
        Wallet wallet = getOrRequireWallet(userId);
        return walletTransactionRepository.findByWalletIdAndTypeOrderByCreatedAtDesc(wallet.getId(), type);
    }

    // -- Private helpers ---------------------------------------

    private Wallet getOrRequireWallet(UUID userId) {
        return walletRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("WALLET_NOT_FOUND",
                        "Wallet not found for user", HttpStatus.NOT_FOUND));
    }

    private void assertSufficientAvailable(Wallet wallet, BigDecimal required) {
        if (wallet.getAvailableBalance().compareTo(required) < 0) {
            throw new BusinessException("INSUFFICIENT_WALLET_BALANCE",
                    "Insufficient wallet balance. Available: ?" + wallet.getAvailableBalance()
                            + ", Required: ?" + required,
                    HttpStatus.BAD_REQUEST);
        }
    }

    private void recordTransaction(UUID walletId, UUID orderId, String razorpayPaymentId,
                                   WalletTransactionType type, BigDecimal amount, String description) {
        WalletTransaction txn = new WalletTransaction();
        txn.setWalletId(walletId);
        txn.setOrderId(orderId);
        txn.setRazorpayPaymentId(razorpayPaymentId);
        txn.setType(type);
        txn.setAmount(amount);
        txn.setDescription(description);
        walletTransactionRepository.save(txn);
    }
}

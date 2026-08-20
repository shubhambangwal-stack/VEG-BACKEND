package com.veggofresh.payment.service.impl;

import com.veggofresh.payment.dto.WalletBalanceDto;
import com.veggofresh.payment.dto.WalletTransactionDto;
import com.veggofresh.payment.entity.Wallet;
import com.veggofresh.payment.entity.WalletTransaction;
import com.veggofresh.payment.entity.WalletTransactionType;
import com.veggofresh.payment.service.WalletTransactionReason;
import com.veggofresh.payment.repository.WalletRepository;
import com.veggofresh.payment.repository.WalletTransactionRepository;
import com.veggofresh.payment.service.WalletService;
import com.veggofresh.platform.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class WalletServiceImpl implements WalletService {

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;

    @Override
    @Transactional(readOnly = true)
    public WalletBalanceDto getBalance(UUID userId) {
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseGet(() -> {
                    Wallet newWallet = new Wallet();
                    newWallet.setUserId(userId);
                    newWallet.setBalance(BigDecimal.ZERO);
                    return newWallet;
                });
        // Not persisted here on the read path -- a balance check shouldn't have the
        // side effect of creating a row. First real credit()/debit() call creates it
        // for real, via getOrCreateWalletForUpdate() below.
        return WalletBalanceDto.builder()
                .userId(userId)
                .balance(wallet.getBalance())
                .build();
    }

    @Override
    public WalletTransactionDto credit(UUID userId, BigDecimal amount, WalletTransactionReason reason,
                                        UUID referenceId, String description) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("WALLET_INVALID_AMOUNT", "Credit amount must be positive", HttpStatus.BAD_REQUEST);
        }

        Wallet wallet = getOrCreateWalletForUpdate(userId);
        wallet.setBalance(wallet.getBalance().add(amount));
        Wallet saved = walletRepository.save(wallet);

        WalletTransaction txn = buildTransaction(userId, WalletTransactionType.CREDIT, reason, amount,
                saved.getBalance(), referenceId, description);
        WalletTransaction savedTxn = walletTransactionRepository.save(txn);

        log.info("Wallet credited: userId={} amount={} reason={} newBalance={}", userId, amount, reason, saved.getBalance());
        return mapToDto(savedTxn);
    }

    @Override
    public WalletTransactionDto debit(UUID userId, BigDecimal amount, WalletTransactionReason reason,
                                       UUID referenceId, String description) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("WALLET_INVALID_AMOUNT", "Debit amount must be positive", HttpStatus.BAD_REQUEST);
        }

        Wallet wallet = getOrCreateWalletForUpdate(userId);
        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new BusinessException("WALLET_INSUFFICIENT_BALANCE",
                    "Wallet balance is insufficient for this debit", HttpStatus.BAD_REQUEST);
        }

        wallet.setBalance(wallet.getBalance().subtract(amount));
        Wallet saved = walletRepository.save(wallet);

        WalletTransaction txn = buildTransaction(userId, WalletTransactionType.DEBIT, reason, amount,
                saved.getBalance(), referenceId, description);
        WalletTransaction savedTxn = walletTransactionRepository.save(txn);

        log.info("Wallet debited: userId={} amount={} reason={} newBalance={}", userId, amount, reason, saved.getBalance());
        return mapToDto(savedTxn);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<WalletTransactionDto> getTransactionHistory(UUID userId, Pageable pageable) {
        return walletTransactionRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(this::mapToDto);
    }

    // ─────────────────────────────────────────────────────────────────────
    // PRIVATE HELPERS
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Row-locked getOrCreate. The PESSIMISTIC_WRITE query only finds EXISTING rows --
     * for a brand-new wallet there's nothing to lock yet, so creation itself is safe
     * from the usual "two concurrent first-writes" race by virtue of the unique
     * constraint on user_id: if two requests somehow both try to create the same
     * user's wallet concurrently, the loser's INSERT fails on the unique constraint
     * rather than silently creating a duplicate wallet -- surfaced as a clear
     * WALLET_CREATE_CONFLICT rather than corrupt state.
     */
    private Wallet getOrCreateWalletForUpdate(UUID userId) {
        return walletRepository.findByUserIdForUpdate(userId)
                .orElseGet(() -> {
                    try {
                        Wallet newWallet = new Wallet();
                        newWallet.setUserId(userId);
                        newWallet.setBalance(BigDecimal.ZERO);
                        return walletRepository.saveAndFlush(newWallet);
                    } catch (org.springframework.dao.DataIntegrityViolationException e) {
                        // Lost a genuine concurrent creation race -- the other request's
                        // wallet now exists for real; lock and use it instead of failing.
                        return walletRepository.findByUserIdForUpdate(userId)
                                .orElseThrow(() -> new BusinessException("WALLET_CREATE_CONFLICT",
                                        "Could not create or lock wallet", HttpStatus.CONFLICT));
                    }
                });
    }

    private WalletTransaction buildTransaction(UUID userId, WalletTransactionType type, WalletTransactionReason reason,
                                                BigDecimal amount, BigDecimal balanceAfter, UUID referenceId, String description) {
        WalletTransaction txn = new WalletTransaction();
        txn.setUserId(userId);
        txn.setType(type);
        txn.setReason(reason);
        txn.setAmount(amount);
        txn.setBalanceAfter(balanceAfter);
        txn.setReferenceId(referenceId);
        txn.setDescription(description);
        return txn;
    }

    private WalletTransactionDto mapToDto(WalletTransaction txn) {
        return WalletTransactionDto.builder()
                .id(txn.getId())
                .type(txn.getType().name())
                .reason(txn.getReason().name())
                .amount(txn.getAmount())
                .balanceAfter(txn.getBalanceAfter())
                .referenceId(txn.getReferenceId())
                .description(txn.getDescription())
                .createdAt(txn.getCreatedAt())
                .build();
    }
}

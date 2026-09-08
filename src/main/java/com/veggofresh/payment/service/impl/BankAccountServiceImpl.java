package com.veggofresh.payment.service.impl;

import com.veggofresh.payment.dto.UserBankAccountDto;
import com.veggofresh.payment.entity.UserBankAccount;
import com.veggofresh.payment.repository.UserBankAccountRepository;
import com.veggofresh.payment.service.BankAccountService;
import com.veggofresh.platform.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class BankAccountServiceImpl implements BankAccountService {

    private final UserBankAccountRepository bankAccountRepository;

    @Override
    public UserBankAccountDto saveOrUpdateBankAccount(UUID userId, UserBankAccountDto dto) {
        if (dto.getAccountHolderName() == null || dto.getAccountHolderName().isBlank()) {
            throw new BusinessException("BANK_ACCOUNT_NAME_REQUIRED", "Account holder name is required", HttpStatus.BAD_REQUEST);
        }
        if (dto.getAccountNumber() == null || dto.getAccountNumber().isBlank()) {
            throw new BusinessException("BANK_ACCOUNT_NUMBER_REQUIRED", "Account number is required", HttpStatus.BAD_REQUEST);
        }
        if (dto.getIfscCode() == null || dto.getIfscCode().isBlank()) {
            throw new BusinessException("BANK_ACCOUNT_IFSC_REQUIRED", "IFSC code is required", HttpStatus.BAD_REQUEST);
        }

        UserBankAccount account = bankAccountRepository.findByUserId(userId)
                .orElseGet(() -> {
                    UserBankAccount newAcc = new UserBankAccount();
                    newAcc.setUserId(userId);
                    return newAcc;
                });

        account.setAccountHolderName(dto.getAccountHolderName().trim());
        account.setAccountNumber(dto.getAccountNumber().trim());
        account.setIfscCode(dto.getIfscCode().trim().toUpperCase());
        account.setBankName(dto.getBankName() != null ? dto.getBankName().trim() : null);
        account.setUpiId(dto.getUpiId() != null ? dto.getUpiId().trim() : null);
        // Reset verification status whenever details are updated — admin must re-verify
        account.setVerified(false);

        UserBankAccount saved = bankAccountRepository.save(account);
        log.info("Bank account saved for userId={} — verification reset, pending admin review", userId);
        return mapToDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public UserBankAccountDto getBankAccountByUserId(UUID userId) {
        return bankAccountRepository.findByUserId(userId)
                .map(this::mapToDto)
                .orElseThrow(() -> new BusinessException("BANK_ACCOUNT_NOT_FOUND",
                        "No bank account details saved for this user", HttpStatus.NOT_FOUND));
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserBankAccountDto> getPendingBankAccounts() {
        return bankAccountRepository.findAllByIsVerified(false)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    public UserBankAccountDto verifyBankAccount(UUID bankAccountId, boolean approve) {
        UserBankAccount account = bankAccountRepository.findById(bankAccountId)
                .orElseThrow(() -> new BusinessException("BANK_ACCOUNT_NOT_FOUND",
                        "Bank account not found with id: " + bankAccountId, HttpStatus.NOT_FOUND));

        account.setVerified(approve);
        UserBankAccount saved = bankAccountRepository.save(account);
        log.info("Bank account {} for userId={} — admin set verified={}",
                bankAccountId, account.getUserId(), approve);
        return mapToDto(saved);
    }

    private UserBankAccountDto mapToDto(UserBankAccount acc) {
        return UserBankAccountDto.builder()
                .id(acc.getId())
                .userId(acc.getUserId())
                .accountHolderName(acc.getAccountHolderName())
                .accountNumber(acc.getAccountNumber())
                .ifscCode(acc.getIfscCode())
                .bankName(acc.getBankName())
                .upiId(acc.getUpiId())
                .isVerified(acc.isVerified())
                .createdAt(acc.getCreatedAt())
                .updatedAt(acc.getUpdatedAt())
                .build();
    }
}

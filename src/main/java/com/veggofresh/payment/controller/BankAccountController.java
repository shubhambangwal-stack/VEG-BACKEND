package com.veggofresh.payment.controller;

import com.veggofresh.payment.dto.UserBankAccountDto;
import com.veggofresh.payment.service.BankAccountService;
import com.veggofresh.platform.common.ApiResponse;
import com.veggofresh.platform.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/bank-account")
@RequiredArgsConstructor
public class BankAccountController {

    private final BankAccountService bankAccountService;

    @PostMapping
    public ResponseEntity<ApiResponse<UserBankAccountDto>> saveBankAccount(@Valid @RequestBody UserBankAccountDto dto) {
        UUID userId = SecurityUtils.getCurrentUserId();
        UserBankAccountDto saved = bankAccountService.saveOrUpdateBankAccount(userId, dto);
        return ResponseEntity.ok(ApiResponse.success(saved, "Bank account details saved successfully"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<UserBankAccountDto>> getBankAccount() {
        UUID userId = SecurityUtils.getCurrentUserId();
        UserBankAccountDto account = bankAccountService.getBankAccountByUserId(userId);
        return ResponseEntity.ok(ApiResponse.success(account, "Bank account details retrieved successfully"));
    }
}

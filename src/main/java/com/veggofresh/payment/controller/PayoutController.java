package com.veggofresh.payment.controller;

import com.veggofresh.payment.dto.PayoutRequestCreateDto;
import com.veggofresh.payment.dto.PayoutResponseDto;
import com.veggofresh.payment.service.PayoutService;
import com.veggofresh.platform.common.ApiResponse;
import com.veggofresh.platform.common.PageResponse;
import com.veggofresh.platform.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payouts")
@RequiredArgsConstructor
public class PayoutController {

    private final PayoutService payoutService;

    @PostMapping("/request")
    public ResponseEntity<ApiResponse<PayoutResponseDto>> requestPayout(@Valid @RequestBody PayoutRequestCreateDto dto) {
        UUID userId = SecurityUtils.getCurrentUserId();
        String role = "VENDOR";
        PayoutResponseDto response = payoutService.requestPayout(userId, role, dto);
        return ResponseEntity.ok(ApiResponse.success(response, "Payout withdrawal request submitted successfully"));
    }

    @GetMapping("/my-requests")
    public ResponseEntity<ApiResponse<PageResponse<PayoutResponseDto>>> getMyPayoutRequests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID userId = SecurityUtils.getCurrentUserId();
        Page<PayoutResponseDto> result = payoutService.getMyPayoutRequests(userId, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(PageResponse.of(result), "Payout requests retrieved successfully"));
    }
}

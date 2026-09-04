package com.veggofresh.payment.controller;

import com.veggofresh.payment.dto.AdminPayoutActionDto;
import com.veggofresh.payment.dto.PayoutResponseDto;
import com.veggofresh.payment.entity.PayoutRequestStatus;
import com.veggofresh.payment.service.PayoutService;
import com.veggofresh.platform.common.ApiResponse;
import com.veggofresh.platform.common.PageResponse;
import com.veggofresh.platform.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/payouts")
@RequiredArgsConstructor
public class AdminPayoutController {

    private final PayoutService payoutService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<PayoutResponseDto>>> getAdminPayoutRequests(
            @RequestParam(required = false) PayoutRequestStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<PayoutResponseDto> result = payoutService.getAdminPayoutRequests(status, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(PageResponse.of(result), "Payout requests retrieved successfully"));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<PayoutResponseDto>> approvePayout(
            @PathVariable UUID id,
            @RequestBody(required = false) AdminPayoutActionDto dto) {
        UUID adminUserId = SecurityUtils.getCurrentUserId();
        PayoutResponseDto response = payoutService.approvePayout(id, adminUserId, dto);
        return ResponseEntity.ok(ApiResponse.success(response, "Payout request approved and processing initiated"));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<ApiResponse<PayoutResponseDto>> rejectPayout(
            @PathVariable UUID id,
            @RequestBody(required = false) AdminPayoutActionDto dto) {
        UUID adminUserId = SecurityUtils.getCurrentUserId();
        PayoutResponseDto response = payoutService.rejectPayout(id, adminUserId, dto);
        return ResponseEntity.ok(ApiResponse.success(response, "Payout request rejected and wallet balance refunded"));
    }
}

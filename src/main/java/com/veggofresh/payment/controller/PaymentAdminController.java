package com.veggofresh.payment.controller;

import com.veggofresh.payment.dto.AdminPayoutActionRequestDto;
import com.veggofresh.payment.dto.PayoutRequestDto;
import com.veggofresh.payment.service.PayoutService;
import com.veggofresh.platform.common.ApiResponse;
import com.veggofresh.platform.common.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Admin-only payment management endpoints:
 * - GET  /api/admin/payment/payouts/pending          -- payout request queue
 * - POST /api/admin/payment/payouts/{id}/process     -- approve or reject
 */
@RestController
@RequestMapping("/api/admin/payment")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class    PaymentAdminController {

    private final PayoutService payoutService;

    @GetMapping("/payouts/pending")
    public ResponseEntity<ApiResponse<PageResponse<PayoutRequestDto>>> getPendingPayouts(
            @PageableDefault(size = 20) Pageable pageable) {
        var page = payoutService.getPendingPayoutRequests(pageable);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.of(page), "Pending payouts retrieved"));
    }

    @PostMapping("/payouts/{payoutRequestId}/process")
    public ResponseEntity<ApiResponse<PayoutRequestDto>> processPayout(
            @PathVariable UUID payoutRequestId,
            @Valid @RequestBody AdminPayoutActionRequestDto request) {
        boolean approve = "APPROVE".equalsIgnoreCase(request.getAction());
        PayoutRequestDto result = payoutService.adminProcessPayout(payoutRequestId, approve, request.getNotes());
        return ResponseEntity.ok(ApiResponse.success(result, "Payout processed"));
    }
}

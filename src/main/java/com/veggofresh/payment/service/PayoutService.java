package com.veggofresh.payment.service;

import com.veggofresh.payment.dto.AdminPayoutActionDto;
import com.veggofresh.payment.dto.PayoutRequestCreateDto;
import com.veggofresh.payment.dto.PayoutResponseDto;
import com.veggofresh.payment.entity.PayoutRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;
import java.util.UUID;

public interface PayoutService {

    PayoutResponseDto requestPayout(UUID userId, String userRole, PayoutRequestCreateDto dto);

    Page<PayoutResponseDto> getMyPayoutRequests(UUID userId, Pageable pageable);

    Page<PayoutResponseDto> getAdminPayoutRequests(PayoutRequestStatus status, Pageable pageable);

    PayoutResponseDto approvePayout(UUID payoutRequestId, UUID adminUserId, AdminPayoutActionDto dto);

    PayoutResponseDto rejectPayout(UUID payoutRequestId, UUID adminUserId, AdminPayoutActionDto dto);

    void handleRazorpayXWebhook(String eventType, Map<String, Object> payload);
}

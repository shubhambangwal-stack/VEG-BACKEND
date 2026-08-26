package com.veggofresh.payment.service;

import com.veggofresh.payment.dto.PayoutRequestDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Handles vendor and delivery partner withdrawal requests. Real Razorpay Payout
 * calls are gated behind {@code payoutsEnabled=true} (requires Razorpay KYC/Route
 * activation). Until then, approved requests go through a manual bank transfer
 * queue managed via the admin portal -- this service handles the approval state
 * machine and wallet debit in both cases.
 */
public interface PayoutService {

    /**
     * Creates a withdrawal request. Debits the wallet immediately on submission
     * (funds are held), so the balance never goes negative for pending requests.
     *
     * @param userId user requesting the withdrawal (vendor or delivery partner)
     * @param amount amount to withdraw in rupees
     * @return the created payout request DTO
     */
    PayoutRequestDto requestWithdrawal(UUID userId, BigDecimal amount);

    /** Returns the calling user's payout request history. */
    Page<PayoutRequestDto> getMyPayoutRequests(UUID userId, Pageable pageable);

    /**
     * Admin-only: approve or reject a pending payout request.
     *
     * @param payoutRequestId the payout request id
     * @param approve         true to approve, false to reject
     * @param adminNotes      optional admin notes
     */
    PayoutRequestDto adminProcessPayout(UUID payoutRequestId, boolean approve, String adminNotes);

    /** Admin-only: list all pending payout requests. */
    Page<PayoutRequestDto> getPendingPayoutRequests(Pageable pageable);
}

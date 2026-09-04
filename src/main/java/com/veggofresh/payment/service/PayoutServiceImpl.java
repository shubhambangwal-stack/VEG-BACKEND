package com.veggofresh.payment.service;

import com.veggofresh.payment.dto.PayoutRequestDto;
import com.veggofresh.payment.entity.PayoutRequest;
import com.veggofresh.payment.entity.PayoutRequestStatus;
import com.veggofresh.payment.repository.PayoutRequestRepository;
import com.veggofresh.platform.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PayoutServiceImpl implements PayoutService {

    private final PayoutRequestRepository payoutRequestRepository;
    private final WalletService walletService;
    private final com.veggofresh.payment.client.RazorpayClient razorpayClient;
    private final com.veggofresh.payment.config.RazorpayProperties razorpayProperties;

    @Override
    @Transactional
    public PayoutRequestDto requestWithdrawal(UUID userId, BigDecimal amount) {
        // Debit wallet immediately -- funds held while request is pending
        walletService.debit(userId, amount, WalletTransactionReason.PAYOUT_DEBIT, null,
                "Withdrawal request for ₹" + amount);

        PayoutRequest request = new PayoutRequest();
        request.setUserId(userId);
        request.setAmount(amount);
        request.setStatus(PayoutRequestStatus.PENDING);
        PayoutRequest saved = payoutRequestRepository.save(request);

        log.info("Withdrawal request created: userId={} amount={} payoutRequestId={}", userId, amount, saved.getId());
        return mapToDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PayoutRequestDto> getMyPayoutRequests(UUID userId, Pageable pageable) {
        return payoutRequestRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(this::mapToDto);
    }

    @Override
    @Transactional
    public PayoutRequestDto adminProcessPayout(UUID payoutRequestId, boolean approve, String adminNotes) {
        PayoutRequest request = payoutRequestRepository.findById(payoutRequestId)
                .orElseThrow(() -> new BusinessException("PAYOUT_REQUEST_NOT_FOUND",
                        "Payout request not found", HttpStatus.NOT_FOUND));

        if (request.getStatus() != PayoutRequestStatus.PENDING) {
            throw new BusinessException("PAYOUT_REQUEST_NOT_PENDING",
                    "This request is already " + request.getStatus(), HttpStatus.BAD_REQUEST);
        }

        if (approve) {
            request.setStatus(PayoutRequestStatus.APPROVED);
            request.setAdminNotes(adminNotes);
            request.setProcessedAt(Instant.now());
            payoutRequestRepository.save(request);

            // NOTE: When payoutsEnabled=true (post-KYC), call RazorpayPayoutClient here.
            if (razorpayProperties.isPayoutsEnabled()) {
                try {
                    // In a fully real scenario, this would come from the Vendor or Delivery partner's profile
                    // For now, if no bank account is integrated, we use a placeholder that the test mode accepts
                    String fundAccountId = "fa_00000000000001"; 
                    String payoutId = razorpayClient.createPayout(fundAccountId, request.getAmount(), "INR", request.getId().toString());
                    request.setRazorpayPayoutId(payoutId);
                    payoutRequestRepository.save(request);
                    log.info("Razorpay Payout {} created for PayoutRequest {}", payoutId, payoutRequestId);
                } catch (Exception e) {
                    log.error("Failed to initiate Razorpay payout for request {}", payoutRequestId, e);
                    throw new BusinessException("PAYOUT_FAILED", "Failed to initiate transfer with Razorpay: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
                }
            } else {
                log.info("Payout request {} approved. Manual bank transfer required (payoutsEnabled=false). Amount={}",
                        payoutRequestId, request.getAmount());
            }
        } else {
            // Rejected -- reverse the wallet debit
            request.setStatus(PayoutRequestStatus.REJECTED);
            request.setAdminNotes(adminNotes);
            request.setProcessedAt(Instant.now());
            payoutRequestRepository.save(request);

            walletService.credit(request.getUserId(), request.getAmount(),
                    WalletTransactionReason.PAYOUT_REVERSAL, payoutRequestId,
                    "Withdrawal request rejected -- amount reversed to wallet");
            log.info("Payout request {} rejected. Wallet credit reversed: userId={} amount={}",
                    payoutRequestId, request.getUserId(), request.getAmount());
        }

        return mapToDto(request);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PayoutRequestDto> getPendingPayoutRequests(Pageable pageable) {
        return payoutRequestRepository.findByStatusOrderByCreatedAtDesc(PayoutRequestStatus.PENDING, pageable)
                .map(this::mapToDto);
    }

    private PayoutRequestDto mapToDto(PayoutRequest r) {
        return PayoutRequestDto.builder()
                .id(r.getId())
                .amount(r.getAmount())
                .status(r.getStatus().name())
                .adminNotes(r.getAdminNotes())
                .createdAt(r.getCreatedAt())
                .processedAt(r.getProcessedAt())
                .razorpayPayoutId(r.getRazorpayPayoutId())
                .build();
    }
}

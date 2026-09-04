package com.veggofresh.payment.controller;

import com.veggofresh.payment.dto.PaymentOrderStatusDto;
import com.veggofresh.payment.dto.VerifyPaymentRequestDto;
import com.veggofresh.payment.entity.PaymentOrder;
import com.veggofresh.payment.entity.PaymentOrderLine;
import com.veggofresh.payment.repository.PaymentOrderLineRepository;
import com.veggofresh.payment.repository.PaymentOrderRepository;
import com.veggofresh.payment.service.PaymentService;
import com.veggofresh.platform.common.ApiResponse;
import com.veggofresh.platform.exception.BusinessException;
import com.veggofresh.platform.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Customer-facing payment endpoints:
 * - POST /api/payment/orders/verify  -- after Razorpay Checkout.js success
 * - GET  /api/payment/orders/{id}    -- status polling
 */
@RestController
@RequestMapping("/api/payment/orders")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final PaymentOrderRepository paymentOrderRepository;
    private final PaymentOrderLineRepository paymentOrderLineRepository;

    /**
     * Called by the frontend immediately after Razorpay Checkout.js reports a
     * successful payment. Verifies the HMAC-SHA256 signature and marks the
     * PaymentOrder as AUTHORIZED so the vendor-accept flow can proceed.
     */
    @PostMapping("/verify")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<String>> verifyPayment(@Valid @RequestBody VerifyPaymentRequestDto request) {
        paymentService.verifyPayment(
                request.getRazorpayOrderId(),
                request.getRazorpayPaymentId(),
                request.getRazorpaySignature()
        );
        return ResponseEntity.ok(ApiResponse.success("Payment verified and authorized successfully", "OK"));
    }

    /**
     * Status check for a specific payment order -- lets the frontend poll for
     * AUTHORIZED/CAPTURED/VOIDED state without needing a webhook to push to it.
     */
    @GetMapping("/{paymentOrderId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<PaymentOrderStatusDto>> getPaymentStatus(
            @PathVariable UUID paymentOrderId) {
        UUID userId = SecurityUtils.getCurrentUserId();

        PaymentOrder po = paymentOrderRepository.findById(paymentOrderId)
                .orElseThrow(() -> new BusinessException("PAYMENT_ORDER_NOT_FOUND",
                        "Payment order not found", HttpStatus.NOT_FOUND));

        if (!po.getUserId().equals(userId)) {
            throw new BusinessException("PAYMENT_ORDER_ACCESS_DENIED",
                    "Access denied to this payment order", HttpStatus.FORBIDDEN);
        }

        List<PaymentOrderLine> lines = paymentOrderLineRepository.findByPaymentOrderId(paymentOrderId);
        List<PaymentOrderStatusDto.PaymentOrderLineStatusDto> lineDtos = lines.stream()
                .map(l -> PaymentOrderStatusDto.PaymentOrderLineStatusDto.builder()
                        .orderId(l.getOrderId())
                        .amount(l.getAmount())
                        .status(l.getStatus().name())
                        .build())
                .collect(Collectors.toList());

        PaymentOrderStatusDto dto = PaymentOrderStatusDto.builder()
                .paymentOrderId(po.getId())
                .razorpayOrderId(po.getRazorpayOrderId())
                .status(po.getStatus().name())
                .totalAmount(po.getTotalAmount())
                .capturedAmount(po.getCapturedAmount())
                .currency(po.getCurrency())
                .authorizedAt(po.getAuthorizedAt())
                .capturedAt(po.getCapturedAt())
                .lines(lineDtos)
                .build();

        return ResponseEntity.ok(ApiResponse.success(dto, "Payment order status retrieved"));
    }
}

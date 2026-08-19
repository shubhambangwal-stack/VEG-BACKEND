package com.veggofresh.payment.controller;

import com.veggofresh.payment.dto.request.CreatePaymentOrderRequest;
import com.veggofresh.payment.dto.request.VerifyPaymentRequest;
import com.veggofresh.payment.dto.response.PaymentOrderResponse;
import com.veggofresh.payment.dto.response.PaymentVerifyResponse;
import com.veggofresh.payment.entity.Payment;
import com.veggofresh.payment.service.PaymentService;
import com.veggofresh.platform.common.ApiResponse;
import com.veggofresh.platform.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Payment Controller � Authorize/Hold/Capture/Void + Wallet.
 *
 * <pre>
 * POST /api/payment/orders             ? Create Razorpay order (Hold) [CUSTOMER]
 * POST /api/payment/verify             ? Verify signature ? AUTHORIZED  [CUSTOMER]
 * POST /api/payment/capture/{orderId}  ? Capture (Vendor Accepts)       [VENDOR/ADMIN]
 * POST /api/payment/void/{orderId}     ? Void (Timeout)                 [SYSTEM/ADMIN]
 * POST /api/payment/webhook            ? Razorpay webhook               [PUBLIC, sig-gated]
 * GET  /api/payment/orders/{orderId}   ? Payment history for order      [CUSTOMER]
 * </pre>
 */
@Slf4j
@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    // -- Step 1: Create Hold -----------------------------------

    @PostMapping("/orders")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<PaymentOrderResponse>> createPaymentOrder(
            @Valid @RequestBody CreatePaymentOrderRequest request) {
        PaymentOrderResponse response = paymentService.createPaymentOrder(
                SecurityUtils.getCurrentUserId(), request);
        return ResponseEntity.ok(ApiResponse.success(response, "Payment order created (hold placed)"));
    }

    // -- Step 2: Verify (AUTHORIZED state) --------------------

    @PostMapping("/verify")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<PaymentVerifyResponse>> verifyPayment(
            @Valid @RequestBody VerifyPaymentRequest request) {
        PaymentVerifyResponse response = paymentService.verifyPayment(
                SecurityUtils.getCurrentUserId(), request);
        return ResponseEntity.ok(ApiResponse.success(response,
                "Payment authorized. Awaiting vendor acceptance to finalize."));
    }

    // -- Step 3a: Capture (Vendor Accepts) --------------------
    // NOTE: In production, this is called internally from VendorOrderManagementService,
    // not directly by the client. This endpoint exists for Admin override + testing.

    @PostMapping("/capture/{orderId}")
    @PreAuthorize("hasAnyRole('VENDOR', 'ADMIN')")
    public ResponseEntity<ApiResponse<String>> capturePayment(@PathVariable UUID orderId) {
        // Commission % hardcoded to 10 here � replace with Admin config value once available
        paymentService.capturePayment(orderId, SecurityUtils.getCurrentUserId(),
                java.math.BigDecimal.valueOf(5.00), java.math.BigDecimal.valueOf(10.00));
        return ResponseEntity.ok(ApiResponse.success("captured", "Payment captured. Order placed."));
    }

    // -- Step 3b: Void (Timeout � no vendor accepted) ---------
    // NOTE: In production, called by the timeout scheduler, not by the client.

    @PostMapping("/void/{orderId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> voidPayment(@PathVariable UUID orderId) {
        paymentService.voidPayment(orderId);
        return ResponseEntity.ok(ApiResponse.success("voided",
                "Payment hold released. Order cancelled. Wallet refunded if applicable."));
    }

    // -- Webhook (Public, HMAC-gated) -------------------------

    @PostMapping("/webhook")
    public ResponseEntity<Void> handleWebhook(
            @RequestBody String payload,
            @RequestHeader("X-Razorpay-Signature") String signature) {
        log.debug("Razorpay webhook received, sig={}...", signature.substring(0, Math.min(8, signature.length())));
        paymentService.handleWebhook(payload, signature);
        return ResponseEntity.ok().build();
    }

    // -- Payment History ---------------------------------------

    @GetMapping("/orders/{orderId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<List<Payment>>> getPaymentsByOrder(
            @PathVariable UUID orderId) {
        List<Payment> payments = paymentService.getPaymentsByOrder(
                orderId, SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success(payments, "Payment history retrieved"));
    }
}

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
 * Payment Controller — Razorpay integration endpoints.
 *
 * <h3>Endpoint summary</h3>
 * <pre>
 * POST /api/payment/orders          → Create Razorpay order + VegGoFresh PAYMENT_PENDING order [JWT]
 * POST /api/payment/verify          → Verify signature + place order + clear cart            [JWT]
 * POST /api/payment/webhook         → Razorpay webhook handler (idempotent)          [PUBLIC — signature-gated]
 * GET  /api/payment/orders/{orderId} → Get payment attempts for a VegGoFresh order            [JWT]
 * </pre>
 *
 * <h3>Security note on /webhook</h3>
 * The webhook endpoint is in {@code PUBLIC_URLS} (no JWT required) because Razorpay
 * does not send Bearer tokens. Security is enforced inside the service layer via
 * HMAC-SHA256 verification of the {@code X-Razorpay-Signature} header against the
 * webhook secret. Any request with an invalid/missing signature returns 401.
 */
@Slf4j
@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * Step 1 of checkout: creates a Razorpay payment order and a VegGoFresh
     * order in PAYMENT_PENDING state.
     *
     * <p>Frontend uses the response to initialize the Razorpay JS checkout popup.
     */
    @PostMapping("/orders")
    public ResponseEntity<ApiResponse<PaymentOrderResponse>> createPaymentOrder(
            @Valid @RequestBody CreatePaymentOrderRequest request) {
        PaymentOrderResponse response = paymentService.createPaymentOrder(
                SecurityUtils.getCurrentUserId(), request);
        return ResponseEntity.ok(ApiResponse.success(response, "Payment order created successfully"));
    }

    /**
     * Step 2 of checkout: verifies Razorpay payment signature and places the order.
     *
     * <p>Called by the frontend after the customer completes payment in the Razorpay
     * popup. On success, the VegGoFresh order moves to PLACED and the cart is cleared.
     */
    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<PaymentVerifyResponse>> verifyPayment(
            @Valid @RequestBody VerifyPaymentRequest request) {
        PaymentVerifyResponse response = paymentService.verifyPayment(
                SecurityUtils.getCurrentUserId(), request);
        return ResponseEntity.ok(ApiResponse.success(response, "Payment verified and order placed successfully"));
    }

    /**
     * Razorpay webhook receiver.
     *
     * <p><b>This endpoint is PUBLIC (no JWT)</b> — secured by HMAC-SHA256 signature
     * verification inside the service using the {@code X-Razorpay-Signature} header.
     * Handles: {@code payment.captured}, {@code payment.failed}.
     * All other events are silently ignored.
     */
    @PostMapping("/webhook")
    public ResponseEntity<Void> handleWebhook(
            @RequestBody String payload,
            @RequestHeader("X-Razorpay-Signature") String signature) {
        log.debug("Razorpay webhook received, signature={}", signature.substring(0, Math.min(8, signature.length())) + "...");
        paymentService.handleWebhook(payload, signature);
        return ResponseEntity.ok().build();
    }

    /**
     * Returns all payment attempts for a VegGoFresh order.
     * Useful for showing "Payment history" or debugging retry scenarios.
     */
    @GetMapping("/orders/{orderId}")
    public ResponseEntity<ApiResponse<List<Payment>>> getPaymentsByOrder(
            @PathVariable UUID orderId) {
        List<Payment> payments = paymentService.getPaymentsByOrder(
                orderId, SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success(payments, "Payment details retrieved successfully"));
    }
}

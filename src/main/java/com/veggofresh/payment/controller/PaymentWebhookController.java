package com.veggofresh.payment.controller;

import com.veggofresh.payment.service.PaymentWebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public webhook endpoint for Razorpay callbacks. No JWT required -- Razorpay
 * authenticates via HMAC-SHA256 signature on the raw body.
 *
 * This endpoint is at /api/public/payment/webhook which matches the
 * SecurityConfig.PUBLIC_URLS pattern /api/public/** so it bypasses JWT auth.
 *
 * IMPORTANT: Spring must receive the raw body string here -- do NOT use
 * @RequestBody with a typed DTO (would re-serialize and break signature).
 */
@Slf4j
@RestController
@RequestMapping("/api/public/payment")
@RequiredArgsConstructor
public class PaymentWebhookController {

    private final PaymentWebhookService paymentWebhookService;

    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(
            @RequestBody String rawPayload,
            @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature) {

        log.info("Razorpay webhook received");

        if (signature == null || signature.isBlank()) {
            log.warn("Razorpay webhook missing X-Razorpay-Signature header");
            return ResponseEntity.badRequest().body("Missing signature header");
        }

        try {
            paymentWebhookService.handleWebhook(rawPayload, signature);
            return ResponseEntity.ok("OK");
        } catch (com.veggofresh.platform.exception.BusinessException e) {
            if ("WEBHOOK_SIGNATURE_INVALID".equals(e.getErrorCode())) {
                return ResponseEntity.status(401).body("Signature invalid");
            }
            log.error("Webhook processing error: {}", e.getMessage());
            // Return 200 anyway to prevent Razorpay retry storm on transient errors
            return ResponseEntity.ok("OK");
        } catch (Exception e) {
            log.error("Unexpected error in webhook handler: {}", e.getMessage(), e);
            // Return 200 to prevent retry storm
            return ResponseEntity.ok("OK");
        }
    }
}

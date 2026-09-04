package com.veggofresh.payment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.veggofresh.payment.client.RazorpayClient;
import com.veggofresh.payment.service.PayoutService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/public/payout")
@RequiredArgsConstructor
public class RazorpayXWebhookController {

    private final PayoutService payoutService;
    private final RazorpayClient razorpayClient;
    private final ObjectMapper objectMapper;

    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(
            @RequestBody String rawPayload,
            @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature) {

        log.info("RazorpayX webhook received");

        if (signature != null && !signature.isBlank()) {
            boolean valid = razorpayClient.verifyWebhookSignature(rawPayload, signature);
            if (!valid) {
                log.warn("Invalid RazorpayX webhook signature");
            }
        }

        try {
            Map<String, Object> payload = objectMapper.readValue(rawPayload, Map.class);
            String event = (String) payload.get("event");
            payoutService.handleRazorpayXWebhook(event, payload);
            return ResponseEntity.ok("OK");
        } catch (Exception e) {
            log.error("Error processing RazorpayX webhook: {}", e.getMessage(), e);
            return ResponseEntity.ok("OK");
        }
    }
}

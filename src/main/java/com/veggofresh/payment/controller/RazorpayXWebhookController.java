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

        // A webhook is an unauthenticated public endpoint: anyone who can reach
        // this port can POST arbitrary JSON and claim a payout was paid or
        // failed. The signature is the only thing distinguishing Razorpay from an
        // attacker, so it is verified BEFORE the payload is parsed and a failure
        // rejects the request.
        //
        // The result used to be computed, logged at WARN, and then ignored, so
        // the endpoint accepted unsigned and forged webhooks alike. A MISSING
        // signature did not even log, which is the quiet half of the same bug.
        if (signature == null || signature.isBlank()) {
            log.warn("RazorpayX webhook received with no X-Razorpay-Signature header -- rejected");
            return ResponseEntity.badRequest().body("Missing signature");
        }
        if (!razorpayClient.verifyWebhookSignature(rawPayload, signature)) {
            log.warn("RazorpayX webhook signature verification FAILED -- rejected");
            return ResponseEntity.badRequest().body("Invalid signature");
        }

        try {
            Map<String, Object> payload = objectMapper.readValue(rawPayload, Map.class);
            String event = (String) payload.get("event");
            payoutService.handleRazorpayXWebhook(event, payload);
            return ResponseEntity.ok("OK");
        } catch (Exception e) {
            log.error("Error processing RazorpayX webhook: {}", e.getMessage(), e);
            // Acknowledge anyway: a non-2xx makes Razorpay retry, so a permanently
            // malformed payload would be replayed until Razorpay gives up. The
            // error is logged, which is where it belongs.
            return ResponseEntity.ok("OK");
        }
    }
}

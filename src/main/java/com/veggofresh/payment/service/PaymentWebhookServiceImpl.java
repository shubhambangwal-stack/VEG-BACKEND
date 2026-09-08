package com.veggofresh.payment.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.veggofresh.notification.entity.NotificationRecipientRole;
import com.veggofresh.notification.entity.NotificationType;
import com.veggofresh.notification.service.NotificationService;
import com.veggofresh.payment.client.RazorpayClient;
import com.veggofresh.payment.entity.PaymentOrder;
import com.veggofresh.payment.entity.PaymentOrderStatus;
import com.veggofresh.payment.entity.PaymentWebhookEvent;
import com.veggofresh.payment.repository.PaymentOrderRepository;
import com.veggofresh.payment.repository.PaymentWebhookEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

/**
 * Razorpay webhook handler. Responsibilities:
 * 1. Verify the HMAC-SHA256 webhook signature (against the raw body).
 * 2. Deduplicate: every event is recorded by razorpay_event_id; a duplicate
 *    event (Razorpay retries on non-2xx) is silently ignored after the first.
 * 3. For {@code payment.authorized}: if the PaymentOrder hasn't been verified
 *    by the customer yet (e.g. Checkout.js callback was missed), mark it AUTHORIZED.
 * 4. Unknown or irrelevant event types are still recorded for the audit trail
 *    but don't change any payment state.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentWebhookServiceImpl implements PaymentWebhookService {

    private final RazorpayClient razorpayClient;
    private final PaymentWebhookEventRepository webhookEventRepository;
    private final PaymentOrderRepository paymentOrderRepository;
    private final ObjectMapper objectMapper;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public void handleWebhook(String rawPayload, String razorpaySignatureHeader) {
        // 1. Signature verification -- MUST happen against the raw body
        if (!razorpayClient.verifyWebhookSignature(rawPayload, razorpaySignatureHeader)) {
            log.warn("Razorpay webhook signature invalid -- rejecting");
            throw new com.veggofresh.platform.exception.BusinessException(
                    "WEBHOOK_SIGNATURE_INVALID", "Webhook signature verification failed",
                    org.springframework.http.HttpStatus.UNAUTHORIZED);
        }

        // 2. Parse event id and type
        String eventId;
        String eventType;
        try {
            JsonNode root = objectMapper.readTree(rawPayload);
            eventId = root.path("id").asText(null);
            eventType = root.path("event").asText(null);
        } catch (Exception e) {
            log.error("Failed to parse Razorpay webhook payload: {}", e.getMessage());
            throw new com.veggofresh.platform.exception.BusinessException(
                    "WEBHOOK_PARSE_ERROR", "Could not parse webhook payload",
                    org.springframework.http.HttpStatus.BAD_REQUEST);
        }

        if (eventId == null || eventType == null) {
            log.warn("Razorpay webhook missing id or event type -- ignoring");
            return;
        }

        // 3. Idempotency: skip if we've seen this event before
        if (webhookEventRepository.existsByRazorpayEventId(eventId)) {
            log.info("Duplicate Razorpay webhook event {} -- already processed, skipping", eventId);
            return;
        }

        // 4. Record the event (even for unhandled types)
        PaymentWebhookEvent record = new PaymentWebhookEvent();
        record.setRazorpayEventId(eventId);
        record.setEventType(eventType);
        record.setPayload(rawPayload);
        webhookEventRepository.save(record);

        // 5. Dispatch to event-specific handler
        try {
            switch (eventType) {
                case "payment.authorized" -> handlePaymentAuthorized(rawPayload);
                case "payment.captured"   -> log.info("Webhook: payment.captured for event {} -- capture already handled by capture API call, no action needed", eventId);
                case "payment.failed"     -> handlePaymentFailed(rawPayload);
                default                   -> log.info("Webhook: unhandled event type '{}' recorded for audit trail", eventType);
            }
            // Mark as processed
            record.setProcessedAt(Instant.now());
            webhookEventRepository.save(record);
        } catch (Exception e) {
            log.error("Error processing webhook event {} (type={}): {}", eventId, eventType, e.getMessage(), e);
            // Don't re-throw -- return 200 to Razorpay so it doesn't retry an event
            // we received but had an internal error on. The audit record is already saved.
        }
    }

    private void handlePaymentAuthorized(String rawPayload) {
        try {
            JsonNode root = objectMapper.readTree(rawPayload);
            JsonNode payment = root.path("payload").path("payment").path("entity");
            String razorpayOrderId = payment.path("order_id").asText(null);
            String razorpayPaymentId = payment.path("id").asText(null);

            if (razorpayOrderId == null || razorpayPaymentId == null) {
                log.warn("payment.authorized webhook missing order_id or payment id");
                return;
            }

            Optional<PaymentOrder> paymentOrderOpt = paymentOrderRepository.findByRazorpayOrderId(razorpayOrderId);
            if (paymentOrderOpt.isEmpty()) {
                log.warn("payment.authorized: no PaymentOrder found for razorpayOrderId={}", razorpayOrderId);
                return;
            }

            PaymentOrder paymentOrder = paymentOrderOpt.get();
            if (paymentOrder.getStatus() == PaymentOrderStatus.CREATED) {
                // Customer's verify() call didn't come through -- webhook is the fallback
                paymentOrder.setRazorpayPaymentId(razorpayPaymentId);
                paymentOrder.setStatus(PaymentOrderStatus.AUTHORIZED);
                paymentOrder.setAuthorizedAt(Instant.now());
                paymentOrderRepository.save(paymentOrder);
                log.info("PaymentOrder {} authorized via webhook (verify call was missed)", paymentOrder.getId());
            } else {
                log.info("PaymentOrder {} already in status {} -- webhook authorization is a no-op", paymentOrder.getId(), paymentOrder.getStatus());
            }
        } catch (Exception e) {
            log.error("Error handling payment.authorized webhook: {}", e.getMessage(), e);
        }
    }

    private void handlePaymentFailed(String rawPayload) {
        try {
            JsonNode root = objectMapper.readTree(rawPayload);
            String razorpayOrderId = root.path("payload").path("payment").path("entity").path("order_id").asText(null);
            if (razorpayOrderId == null) return;

            paymentOrderRepository.findByRazorpayOrderId(razorpayOrderId).ifPresent(po -> {
                if (!po.getStatus().isTerminal()) {
                    po.setStatus(PaymentOrderStatus.FAILED);
                    paymentOrderRepository.save(po);
                    notificationService.send(po.getUserId(), NotificationRecipientRole.CUSTOMER, NotificationType.PAYMENT_FAILED,
                            "Payment failed", "Your payment could not be completed — please try again",
                            "{\"paymentOrderId\":\"" + po.getId() + "\"}");
                    log.warn("PaymentOrder {} marked FAILED via webhook", po.getId());
                }
            });
        } catch (Exception e) {
            log.error("Error handling payment.failed webhook: {}", e.getMessage(), e);
        }
    }
}

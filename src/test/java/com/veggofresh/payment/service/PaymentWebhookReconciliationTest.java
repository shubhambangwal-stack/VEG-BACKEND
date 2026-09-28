package com.veggofresh.payment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.veggofresh.notification.entity.NotificationRecipientRole;
import com.veggofresh.notification.service.NotificationService;
import com.veggofresh.payment.client.RazorpayClient;
import com.veggofresh.payment.entity.PaymentOrder;
import com.veggofresh.payment.entity.PaymentOrderLine;
import com.veggofresh.payment.entity.PaymentOrderStatus;
import com.veggofresh.payment.repository.PaymentOrderLineRepository;
import com.veggofresh.payment.repository.PaymentOrderRepository;
import com.veggofresh.payment.repository.PaymentWebhookEventRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * A failed payment has to reach the orders it was covering.
 *
 * <p>One checkout creates one payment hold spread across N orders, one per cart.
 * The webhook used to mark the PaymentOrder FAILED and stop there, which left all
 * N orders in PLACED -- a state a vendor can accept and delivery can complete.
 * The result was settlements paid out to vendors, drivers and the platform for
 * orders nobody had paid for, while the customer had been told the payment
 * failed.
 *
 * <p>These pin that the order side is actually told, that the N orders are all
 * reported (not just the first), and that a top-up with no orders behind it is
 * not treated as an order failure.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PaymentWebhookReconciliationTest {

    @Mock
    private RazorpayClient razorpayClient;

    @Mock
    private PaymentWebhookEventRepository webhookEventRepository;

    @Mock
    private PaymentOrderRepository paymentOrderRepository;

    @Mock
    private PaymentOrderLineRepository paymentOrderLineRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private PaymentFailureHandler paymentFailureHandler;

    private PaymentWebhookServiceImpl webhookService;

    private final UUID userId = UUID.randomUUID();
    private final UUID paymentOrderId = UUID.randomUUID();
    private final String razorpayOrderId = "order_abc123";

    @BeforeEach
    void setUp() throws Exception {
        webhookService = instantiate(paymentFailureHandler);
        when(razorpayClient.verifyWebhookSignature(anyString(), anyString())).thenReturn(true);
        when(webhookEventRepository.existsByRazorpayEventId(anyString())).thenReturn(false);
        when(webhookEventRepository.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    private PaymentWebhookServiceImpl instantiate(PaymentFailureHandler handler) throws Exception {
        Class<?> type = PaymentWebhookServiceImpl.class;
        Constructor<?> constructor = type.getDeclaredConstructors()[0];
        Class<?>[] types = constructor.getParameterTypes();
        Object[] args = new Object[types.length];
        ObjectMapper mapper = new ObjectMapper();
        for (int i = 0; i < types.length; i++) {
            if (types[i].isAssignableFrom(RazorpayClient.class)) {
                args[i] = razorpayClient;
            } else if (types[i].isAssignableFrom(PaymentWebhookEventRepository.class)) {
                args[i] = webhookEventRepository;
            } else if (types[i].isAssignableFrom(PaymentOrderRepository.class)) {
                args[i] = paymentOrderRepository;
            } else if (types[i].isAssignableFrom(PaymentOrderLineRepository.class)) {
                args[i] = paymentOrderLineRepository;
            } else if (types[i].isAssignableFrom(ObjectMapper.class)) {
                args[i] = mapper;
            } else if (types[i].isAssignableFrom(NotificationService.class)) {
                args[i] = notificationService;
            } else if (types[i].isAssignableFrom(PaymentFailureHandler.class)) {
                args[i] = handler;
            }
        }
        constructor.setAccessible(true);
        return (PaymentWebhookServiceImpl) constructor.newInstance(args);
    }

    private PaymentOrder givenBatch(PaymentOrderStatus status, boolean topup) {
        PaymentOrder po = new PaymentOrder();
        po.setId(paymentOrderId);
        po.setUserId(userId);
        po.setRazorpayOrderId(razorpayOrderId);
        po.setStatus(status);
        po.setTopup(topup);
        when(paymentOrderRepository.findByRazorpayOrderId(razorpayOrderId)).thenReturn(Optional.of(po));
        return po;
    }

    private void givenLinesFor(UUID... orderIds) {
        List<PaymentOrderLine> lines = java.util.Arrays.stream(orderIds).map(orderId -> {
            PaymentOrderLine line = new PaymentOrderLine();
            line.setId(UUID.randomUUID());
            line.setPaymentOrderId(paymentOrderId);
            line.setOrderId(orderId);
            return line;
        }).toList();
        when(paymentOrderLineRepository.findByPaymentOrderId(paymentOrderId)).thenReturn(lines);
    }

    private String failedPayload() {
        return """
                {
                  "id": "evt_failed_1",
                  "event": "payment.failed",
                  "payload": { "payment": { "entity": { "id": "pay_1", "order_id": "%s" } } }
                }
                """.formatted(razorpayOrderId);
    }

    @SuppressWarnings("unchecked")
    private List<UUID> capturedOrderIds() {
        ArgumentCaptor<List<UUID>> captor = ArgumentCaptor.forClass(List.class);
        verify(paymentFailureHandler).onPaymentFailed(any(), any(), captor.capture());
        return captor.getValue();
    }

    // ── the orders must be told ─────────────────────────────────────────────

    @Test
    @DisplayName("A failed payment tells the order module about the payment")
    void failedPaymentNotifiesOrderSide() {
        givenBatch(PaymentOrderStatus.AUTHORIZED, false);
        givenLinesFor(UUID.randomUUID());

        webhookService.handleWebhook(failedPayload(), "sig");

        verify(paymentFailureHandler, times(1)).onPaymentFailed(eq(paymentOrderId), any(), any());
    }

    @Test
    @DisplayName("Every order behind the failed payment is reported, not just one")
    void allOrdersInTheBatchAreReported() {
        givenBatch(PaymentOrderStatus.AUTHORIZED, false);
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        UUID third = UUID.randomUUID();
        givenLinesFor(first, second, third);

        webhookService.handleWebhook(failedPayload(), "sig");

        List<UUID> reported = capturedOrderIds();
        assertEquals(3, reported.size(), "a 3-cart checkout means 3 orders to cancel");
        assertTrue(reported.containsAll(List.of(first, second, third)));
    }

    @Test
    @DisplayName("Duplicate lines for one order are reported once")
    void duplicateOrderIdsAreDeduplicated() {
        givenBatch(PaymentOrderStatus.AUTHORIZED, false);
        UUID only = UUID.randomUUID();
        givenLinesFor(only, only, only);

        webhookService.handleWebhook(failedPayload(), "sig");

        assertEquals(List.of(only), capturedOrderIds());
    }

    @Test
    @DisplayName("A top-up failure never bothers the order side")
    void topupFailureSkipsTheOrderSide() {
        givenBatch(PaymentOrderStatus.AUTHORIZED, true);
        givenLinesFor();

        webhookService.handleWebhook(failedPayload(), "sig");

        // A wallet top-up covers no orders, so there is nothing to reconcile. The
        // handler is not called at all rather than called with an empty list, so
        // the order side never has to special-case a payment that had no orders.
        verify(paymentFailureHandler, never()).onPaymentFailed(any(), any(), any());
    }

    // ── the payment state must still be recorded ────────────────────────────

    @Test
    @DisplayName("The batch is marked FAILED before the order side is told")
    void batchIsMarkedFailed() {
        PaymentOrder po = givenBatch(PaymentOrderStatus.AUTHORIZED, false);
        givenLinesFor(UUID.randomUUID());

        webhookService.handleWebhook(failedPayload(), "sig");

        assertEquals(PaymentOrderStatus.FAILED, po.getStatus());
    }

    @Test
    @DisplayName("An already-terminal batch is not reprocessed")
    void terminalBatchIsNotReprocessed() {
        givenBatch(PaymentOrderStatus.FAILED, false);
        givenLinesFor(UUID.randomUUID());

        webhookService.handleWebhook(failedPayload(), "sig");

        verify(paymentFailureHandler, never()).onPaymentFailed(any(), any(), any());
    }

    // ── failure of the order side must not lose the payment state ───────────

    @Test
    @DisplayName("If the order module throws, the payment is still recorded FAILED")
    void orderSideFailureDoesNotLosePaymentState() throws Exception {
        PaymentOrder po = givenBatch(PaymentOrderStatus.AUTHORIZED, false);
        givenLinesFor(UUID.randomUUID());
        PaymentFailureHandler exploding = (a, b, c) -> {
            throw new IllegalStateException("order module down");
        };
        PaymentWebhookServiceImpl service = instantiate(exploding);

        service.handleWebhook(failedPayload(), "sig");

        assertEquals(PaymentOrderStatus.FAILED, po.getStatus(),
                "the payment must be marked failed even if reconciliation fails");
        verify(notificationService).send(any(), any(), any(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("With no order handler wired, the payment is still recorded FAILED")
    void missingHandlerDoesNotLosePaymentState() throws Exception {
        PaymentOrder po = givenBatch(PaymentOrderStatus.AUTHORIZED, false);
        givenLinesFor(UUID.randomUUID());
        PaymentWebhookServiceImpl service = instantiate(null);

        service.handleWebhook(failedPayload(), "sig");

        assertEquals(PaymentOrderStatus.FAILED, po.getStatus());
    }

}

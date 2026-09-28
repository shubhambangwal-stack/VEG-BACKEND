package com.veggofresh.payment.service;

import com.veggofresh.payment.entity.PaymentOrder;
import com.veggofresh.payment.entity.PaymentOrderLine;
import com.veggofresh.payment.entity.PaymentOrderLineStatus;
import com.veggofresh.payment.entity.PaymentOrderStatus;
import com.veggofresh.payment.repository.PaymentOrderLineRepository;
import com.veggofresh.payment.repository.PaymentOrderRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.lang.reflect.Constructor;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The payment module is the only party that knows whether money was actually
 * collected, so it is the only party allowed to move money on cancellation.
 *
 * <p>These cover {@link PaymentServiceImpl#onOrderCancelled(UUID)}, the single
 * refund authority that the order layer delegates to. The bug they exist to pin
 * down was in the callers, not here: {@code OrderServiceImpl.cancelOrder()},
 * {@code CustomerOrderServiceImpl.cancelOrderSystemInitiated()} and
 * {@code VendorAcceptTimeoutSweepService} each credited the wallet themselves,
 * on top of this method, and did so without checking whether a capture had
 * happened. The assertions below pin the contract those callers depend on, so a
 * future extra credit is caught here.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PaymentCancellationRefundTest {

    @Mock
    private PaymentOrderRepository paymentOrderRepository;

    @Mock
    private PaymentOrderLineRepository paymentOrderLineRepository;

    @Mock
    private WalletService walletService;

    private PaymentServiceImpl paymentService;

    private final UUID userId = UUID.randomUUID();
    private final UUID orderId = UUID.randomUUID();
    private final UUID paymentOrderId = UUID.randomUUID();

    @BeforeEach
    void setUp() throws Exception {
        paymentService = instantiate();
    }

    /** Only the three collaborators this method touches; the rest are null. */
    private PaymentServiceImpl instantiate() throws Exception {
        Class<?> type = PaymentServiceImpl.class;
        Constructor<?> constructor = type.getDeclaredConstructors()[0];
        Class<?>[] types = constructor.getParameterTypes();
        Object[] args = new Object[types.length];
        for (int i = 0; i < types.length; i++) {
            if (types[i].isAssignableFrom(PaymentOrderRepository.class)) {
                args[i] = paymentOrderRepository;
            } else if (types[i].isAssignableFrom(PaymentOrderLineRepository.class)) {
                args[i] = paymentOrderLineRepository;
            } else if (types[i].isAssignableFrom(WalletService.class)) {
                args[i] = walletService;
            }
        }
        constructor.setAccessible(true);
        return (PaymentServiceImpl) constructor.newInstance(args);
    }

    private PaymentOrder batch(PaymentOrderStatus status) {
        PaymentOrder paymentOrder = new PaymentOrder();
        paymentOrder.setId(paymentOrderId);
        paymentOrder.setUserId(userId);
        paymentOrder.setStatus(status);
        paymentOrder.setTotalAmount(new BigDecimal("125"));
        return paymentOrder;
    }

    private PaymentOrderLine line(PaymentOrderLineStatus status) {
        PaymentOrderLine line = new PaymentOrderLine();
        line.setId(UUID.randomUUID());
        line.setPaymentOrderId(paymentOrderId);
        line.setOrderId(orderId);
        line.setAmount(new BigDecimal("125"));
        line.setStatus(status);
        return line;
    }

    private void givenLine(PaymentOrderLine line, PaymentOrder batch) {
        when(paymentOrderLineRepository.findByOrderId(orderId)).thenReturn(Optional.of(line));
        when(paymentOrderRepository.findByIdForUpdate(paymentOrderId)).thenReturn(Optional.of(batch));
    }

    // ── the money was collected, so it must go back ─────────────────────────

    @Test
    @DisplayName("Cancelling after capture credits the customer exactly once")
    void capturedCancellationRefunds() {
        PaymentOrderLine line = line(PaymentOrderLineStatus.ACCEPTED);
        givenLine(line, batch(PaymentOrderStatus.CAPTURED));

        paymentService.onOrderCancelled(orderId);

        verify(walletService, times(1)).credit(eq(userId), eq(new BigDecimal("125")), any(), eq(orderId), anyString());
    }

    @Test
    @DisplayName("A captured cancellation marks the line refunded so it cannot be refunded again")
    void capturedCancellationIsIdempotentViaLineStatus() {
        PaymentOrderLine line = line(PaymentOrderLineStatus.ACCEPTED);
        givenLine(line, batch(PaymentOrderStatus.CAPTURED));

        paymentService.onOrderCancelled(orderId);

        assertEquals(PaymentOrderLineStatus.CANCELLED_REFUNDED, line.getStatus(),
                "the terminal status is what makes a repeated cancel a no-op");
    }

    @Test
    @DisplayName("A second cancellation of an already-refunded line credits nothing")
    void repeatedCancellationDoesNotRefundTwice() {
        PaymentOrderLine line = line(PaymentOrderLineStatus.CANCELLED_REFUNDED);
        givenLine(line, batch(PaymentOrderStatus.CAPTURED));

        paymentService.onOrderCancelled(orderId);

        verify(walletService, never()).credit(any(), any(), any(), any(), anyString());
    }

    // ── the money was never collected, so there is nothing to give back ──────

    @Test
    @DisplayName("Cancelling an uncaptured order credits nothing -- this is the double-refund case")
    void uncapturedCancellationRefundsNothing() {
        PaymentOrderLine line = line(PaymentOrderLineStatus.PENDING);
        givenLine(line, batch(PaymentOrderStatus.AUTHORIZED));

        paymentService.onOrderCancelled(orderId);

        verify(walletService, never()).credit(any(), any(), any(), any(), anyString());
    }

    @Test
    @DisplayName("An uncaptured cancellation voids the line instead")
    void uncapturedCancellationVoids() {
        PaymentOrderLine line = line(PaymentOrderLineStatus.PENDING);
        givenLine(line, batch(PaymentOrderStatus.AUTHORIZED));

        paymentService.onOrderCancelled(orderId);

        assertEquals(PaymentOrderLineStatus.VOIDED, line.getStatus());
    }

    @Test
    @DisplayName("A partially captured batch still refunds the cancelled line")
    void partialCaptureStillRefunds() {
        PaymentOrderLine line = line(PaymentOrderLineStatus.ACCEPTED);
        givenLine(line, batch(PaymentOrderStatus.PARTIALLY_CAPTURED));

        paymentService.onOrderCancelled(orderId);

        verify(walletService, times(1)).credit(eq(userId), eq(new BigDecimal("125")), any(), eq(orderId), anyString());
    }

    // ── nothing to reconcile ────────────────────────────────────────────────

    @Test
    @DisplayName("An order with no payment line credits nothing")
    void orderWithoutPaymentLineRefundsNothing() {
        when(paymentOrderLineRepository.findByOrderId(orderId)).thenReturn(Optional.empty());

        paymentService.onOrderCancelled(orderId);

        verify(walletService, never()).credit(any(), any(), any(), any(), anyString());
    }
}

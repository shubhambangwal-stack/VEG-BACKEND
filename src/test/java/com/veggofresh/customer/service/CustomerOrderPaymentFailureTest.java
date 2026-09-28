package com.veggofresh.customer.service;

import com.veggofresh.customer.entity.Order;
import com.veggofresh.customer.entity.OrderStatus;
import com.veggofresh.customer.repository.OrderRepository;
import com.veggofresh.customer.service.impl.CustomerOrderServiceImpl;
import com.veggofresh.customer.service.impl.OrderResponseMapper;
import com.veggofresh.admin.service.PlatformSettingsService;
import com.veggofresh.notification.entity.NotificationRecipientRole;
import com.veggofresh.notification.service.NotificationService;
import com.veggofresh.payment.service.PaymentService;
import com.veggofresh.vendor.service.ShopLookupService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * A failed payment must take the orders it was covering down with it.
 *
 * <p>One checkout creates one payment hold spread across N orders, one per cart.
 * Until the order side was told about a payment failure, all N orders sat in
 * PLACED -- a state a vendor can accept and delivery can complete -- so
 * settlements were paid out for orders nobody had paid for.
 *
 * <p>These pin that every order in the batch is cancelled, that one failure does
 * not abandon the rest, and that a failed payment is never treated as a refund:
 * the hold was never captured, so there is nothing to give back.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CustomerOrderPaymentFailureTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderService orderService;

    @Mock
    private OrderResponseMapper orderResponseMapper;

    @Mock
    private PaymentService paymentService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private PlatformSettingsService platformSettingsService;

    @Mock
    private ShopLookupService shopLookupService;

    private CustomerOrderServiceImpl service;

    private final UUID userId = UUID.randomUUID();
    private final UUID paymentOrderId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        // A bare mock returns null from save(), and the service acts on the saved
        // result (notifying and voiding the payment line), so it has to come back.
        when(orderRepository.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    private CustomerOrderServiceImpl service() {
        return new CustomerOrderServiceImpl(orderRepository, orderService, orderResponseMapper, paymentService,
                platformSettingsService, notificationService, shopLookupService);
    }

    private Order givenOrder(UUID id, OrderStatus status) {
        Order order = new Order();
        order.setId(id);
        order.setUserId(userId);
        order.setStatus(status);
        order.setOrderNumber("#DM-" + id.toString().substring(0, 8));
        when(orderRepository.findById(id)).thenReturn(Optional.of(order));
        return order;
    }

    // ── every order in the batch must go ─────────────────────────────────────

    @Test
    @DisplayName("A failed payment cancels every order it was covering")
    void allOrdersAreCancelled() {
        service = service();
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        UUID third = UUID.randomUUID();
        givenOrder(first, OrderStatus.PLACED);
        givenOrder(second, OrderStatus.PLACED);
        givenOrder(third, OrderStatus.PLACED);

        service.onPaymentFailed(paymentOrderId, userId, List.of(first, second, third));

        // Three carts means three customers to tell -- one notification per order.
        verify(notificationService, times(3)).send(any(), any(), any(), anyString(), anyString(), anyString());
        verify(paymentService).onOrderCancelled(first);
        verify(paymentService).onOrderCancelled(second);
        verify(paymentService).onOrderCancelled(third);
    }

    @Test
    @DisplayName("Orders in the batch are left CANCELLED")
    void ordersAreLeftCancelled() {
        service = service();
        UUID id = UUID.randomUUID();
        Order order = givenOrder(id, OrderStatus.PLACED);

        service.onPaymentFailed(paymentOrderId, userId, List.of(id));

        assertEquals(OrderStatus.CANCELLED, order.getStatus());
    }

    @Test
    @DisplayName("A customer is told why each order was cancelled")
    void customerIsToldTheReason() {
        service = service();
        UUID id = UUID.randomUUID();
        givenOrder(id, OrderStatus.PLACED);

        service.onPaymentFailed(paymentOrderId, userId, List.of(id));

        verify(notificationService).send(any(), any(), any(), anyString(), contains("payment"), anyString());
    }

    // ── a failed payment is not a refund ────────────────────────────────────

    @Test
    @DisplayName("Payment is still asked to void each line, so no money is invented")
    void paymentSideIsAskedToVoidTheLines() {
        service = service();
        UUID id = UUID.randomUUID();
        givenOrder(id, OrderStatus.PLACED);

        service.onPaymentFailed(paymentOrderId, userId, List.of(id));

        // The payment module decides: an uncaptured hold is voided, not refunded.
        verify(paymentService).onOrderCancelled(id);
    }

    // ── resilience ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("One order failing to cancel does not strand the others")
    void oneFailureDoesNotAbandonTheRest() {
        service = service();
        UUID broken = UUID.randomUUID();
        UUID healthy = UUID.randomUUID();
        Order healthyOrder = givenOrder(healthy, OrderStatus.PLACED);
        when(orderRepository.findById(broken)).thenThrow(new IllegalStateException("db down"));

        service.onPaymentFailed(paymentOrderId, userId, List.of(broken, healthy));

        // Leaving `healthy` in PLACED is the exact bug this path exists to close.
        assertEquals(OrderStatus.CANCELLED, healthyOrder.getStatus());
    }

    @Test
    @DisplayName("A top-up failure with no orders is a no-op")
    void noOrdersIsANoOp() {
        service = service();

        service.onPaymentFailed(paymentOrderId, userId, List.of());

        verify(orderRepository, never()).findById(any());
        verify(notificationService, never()).send(any(), any(), any(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("A null order list is tolerated, not a crash")
    void nullOrderListIsTolerated() {
        service = service();

        service.onPaymentFailed(paymentOrderId, userId, null);

        verify(orderRepository, never()).findById(any());
    }

    // ── idempotency ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("A repeated failure for the same order is a no-op")
    void repeatedFailureIsANoOp() {
        service = service();
        UUID id = UUID.randomUUID();
        givenOrder(id, OrderStatus.CANCELLED);

        service.onPaymentFailed(paymentOrderId, userId, List.of(id));

        // Razorpay retries webhooks; the second pass must not re-notify or re-refund.
        verify(notificationService, never()).send(any(), any(), any(), anyString(), anyString(), anyString());
        verify(paymentService, never()).onOrderCancelled(any());
    }

    @Test
    @DisplayName("A repeated failure for several already-cancelled orders notifies nobody")
    void repeatedFailureAcrossBatchIsANoOp() {
        service = service();
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        givenOrder(first, OrderStatus.CANCELLED);
        givenOrder(second, OrderStatus.CANCELLED);

        service.onPaymentFailed(paymentOrderId, userId, List.of(first, second));

        verify(notificationService, never()).send(any(), any(), any(), anyString(), anyString(), anyString());
        verify(paymentService, never()).onOrderCancelled(any());
    }
}

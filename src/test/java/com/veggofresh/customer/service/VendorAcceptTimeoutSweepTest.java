package com.veggofresh.customer.service;

import com.veggofresh.admin.service.PlatformSettingsService;
import com.veggofresh.customer.entity.Order;
import com.veggofresh.customer.entity.OrderStatus;
import com.veggofresh.customer.repository.OrderRepository;
import com.veggofresh.customer.service.impl.VendorAcceptTimeoutSweepService;
import com.veggofresh.payment.service.PaymentService;
import com.veggofresh.payment.service.WalletService;
import com.veggofresh.platform.exception.BusinessException;

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
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The vendor-accept timeout sweep used to credit the order total to the
 * customer's wallet directly, once per timed-out order, without ever asking the
 * payment module whether that order had ever been charged. The result was free
 * money for anyone who simply let an order expire: the hold was created but
 * never captured, the order timed out, and the full total landed in the wallet.
 *
 * <p>The class javadoc described calling {@code PaymentService.onOrderCancelled()}
 * and the {@code PaymentService} import was present, but no field was ever wired
 * to it, so the documented behaviour and the implemented behaviour disagreed.
 * These tests pin the implemented behaviour to the documented one.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class VendorAcceptTimeoutSweepTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private PlatformSettingsService platformSettingsService;

    @Mock
    private PaymentService paymentService;

    @Mock
    private WalletService walletService;

    private VendorAcceptTimeoutSweepService sweep;

    private final UUID userId = UUID.randomUUID();
    private final UUID orderId = UUID.randomUUID();

    @BeforeEach
    void setUp() throws Exception {
        sweep = instantiate();
        when(platformSettingsService.getVendorAcceptTimeoutSeconds()).thenReturn(300);
    }

    private VendorAcceptTimeoutSweepService instantiate() throws Exception {
        Class<?> type = VendorAcceptTimeoutSweepService.class;
        Constructor<?> constructor = type.getDeclaredConstructors()[0];
        Class<?>[] types = constructor.getParameterTypes();
        Object[] args = new Object[types.length];
        for (int i = 0; i < types.length; i++) {
            if (types[i].isAssignableFrom(OrderRepository.class)) {
                args[i] = orderRepository;
            } else if (types[i].isAssignableFrom(PlatformSettingsService.class)) {
                args[i] = platformSettingsService;
            } else if (types[i].isAssignableFrom(PaymentService.class)) {
                args[i] = paymentService;
            } else if (types[i].isAssignableFrom(WalletService.class)) {
                args[i] = walletService;
            }
        }
        constructor.setAccessible(true);
        return (VendorAcceptTimeoutSweepService) constructor.newInstance(args);
    }

    private Order givenTimedOutOrder() {
        Order order = new Order();
        order.setId(orderId);
        order.setUserId(userId);
        order.setOrderNumber("#DM-123456");
        order.setStatus(OrderStatus.PLACED);
        order.setTotalAmount(new BigDecimal("125"));
        order.setCreatedAt(Instant.now().minusSeconds(3600));
        when(orderRepository.findByStatusAndCreatedAtBefore(eq(OrderStatus.PLACED), any()))
                .thenReturn(List.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));
        return order;
    }

    // ── the money decision belongs to the payment module ────────────────────

    @Test
    @DisplayName("A timed-out order is handed to the payment module instead of being refunded here")
    void delegatesTheRefundToThePaymentModule() {
        givenTimedOutOrder();

        sweep.sweepExpiredOrders();

        verify(paymentService, times(1)).onOrderCancelled(orderId);
    }

    @Test
    @DisplayName("The sweep never touches a wallet -- that was the free-money bug")
    void sweepNeverCreditsAWalletDirectly() {
        givenTimedOutOrder();

        sweep.sweepExpiredOrders();

        verify(walletService, never()).credit(any(), any(), any(), any(), anyString());
        verify(walletService, never()).debit(any(), any(), any(), any(), anyString());
    }

    @Test
    @DisplayName("The order is still cancelled even though the refund is delegated")
    void orderIsStillCancelled() {
        Order order = givenTimedOutOrder();

        sweep.sweepExpiredOrders();

        assertEquals(OrderStatus.CANCELLED, order.getStatus());
        assertEquals(orderId, order.getId());
    }

    // ── one bad order must not stop the sweep ───────────────────────────────

    @Test
    @DisplayName("One order failing does not abort the rest of the batch")
    void oneFailureDoesNotStopTheSweep() {
        Order first = givenTimedOutOrder();
        Order second = new Order();
        second.setId(UUID.randomUUID());
        second.setUserId(userId);
        second.setOrderNumber("#DM-654321");
        second.setStatus(OrderStatus.PLACED);
        second.setTotalAmount(new BigDecimal("60"));
        when(orderRepository.findByStatusAndCreatedAtBefore(eq(OrderStatus.PLACED), any()))
                .thenReturn(List.of(first, second));

        doThrow(new BusinessException("PAYMENT_ORDER_NOT_FOUND", "boom"))
                .when(paymentService).onOrderCancelled(first.getId());

        sweep.sweepExpiredOrders();

        verify(paymentService, times(1)).onOrderCancelled(second.getId());
    }

    // ── nothing to do ───────────────────────────────────────────────────────

    @Test
    @DisplayName("An empty sweep asks the payment module nothing")
    void emptySweepDoesNothing() {
        when(orderRepository.findByStatusAndCreatedAtBefore(eq(OrderStatus.PLACED), any()))
                .thenReturn(List.of());

        sweep.sweepExpiredOrders();

        verify(paymentService, never()).onOrderCancelled(any());
    }
}

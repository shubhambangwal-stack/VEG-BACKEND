package com.veggofresh.customer.service;

import com.veggofresh.customer.entity.OrderStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class OrderStateTransitionTest {

    @Test
    void testValidTransitions() {
        // PLACED can go to CONFIRMED or CANCELLED
        assertTrue(OrderStatus.PLACED.isValidTransition(OrderStatus.CONFIRMED));
        assertTrue(OrderStatus.PLACED.isValidTransition(OrderStatus.CANCELLED));

        // CONFIRMED can go to PREPARING or CANCELLED
        assertTrue(OrderStatus.CONFIRMED.isValidTransition(OrderStatus.PREPARING));
        assertTrue(OrderStatus.CONFIRMED.isValidTransition(OrderStatus.CANCELLED));

        // PREPARING can go to OUT_FOR_DELIVERY
        assertTrue(OrderStatus.PREPARING.isValidTransition(OrderStatus.OUT_FOR_DELIVERY));

        // OUT_FOR_DELIVERY can go to DELIVERED
        assertTrue(OrderStatus.OUT_FOR_DELIVERY.isValidTransition(OrderStatus.DELIVERED));
    }

    @Test
    void testInvalidTransitions() {
        // PLACED cannot jump to DELIVERED or OUT_FOR_DELIVERY
        assertFalse(OrderStatus.PLACED.isValidTransition(OrderStatus.DELIVERED));
        assertFalse(OrderStatus.PLACED.isValidTransition(OrderStatus.OUT_FOR_DELIVERY));

        // PREPARING cannot be CANCELLED
        assertFalse(OrderStatus.PREPARING.isValidTransition(OrderStatus.CANCELLED));

        // OUT_FOR_DELIVERY cannot be CANCELLED
        assertFalse(OrderStatus.OUT_FOR_DELIVERY.isValidTransition(OrderStatus.CANCELLED));

        // DELIVERED is terminal, cannot transition anywhere
        assertFalse(OrderStatus.DELIVERED.isValidTransition(OrderStatus.PLACED));
        assertFalse(OrderStatus.DELIVERED.isValidTransition(OrderStatus.CANCELLED));

        // CANCELLED is terminal
        assertFalse(OrderStatus.CANCELLED.isValidTransition(OrderStatus.CONFIRMED));
    }
}

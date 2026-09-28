package com.veggofresh.customer.service;

import com.veggofresh.customer.service.impl.OrderServiceImpl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Order numbers are a customer-facing, unique column, and a duplicate is a hard
 * failure of the whole checkout.
 *
 * <p>One multi-cart checkout creates N orders in a single transaction, and the
 * earlier generation schemes could hand two of them the same number. That failed
 * the INSERT on a {@code unique} column and surfaced to the customer as a 500
 * for every cart in the checkout, not just the unlucky one. These pin that draws
 * stay unique under volume and under concurrency, and that the value still fits
 * the 20-character column.
 */
class OrderNumberGenerationTest {

    private static final String PREFIX = "#DM-";
    private static final int COLUMN_LENGTH = 20;

    private OrderServiceImpl service;

    /**
     * {@code nextOrderNumber()} is a pure function of a static CSPRNG, so it needs
     * no collaborators. The generated constructor is satisfied reflectively with
     * nulls rather than dragging in the whole dependency graph.
     */
    private OrderServiceImpl newService() throws Exception {
        Constructor<?> constructor = OrderServiceImpl.class.getDeclaredConstructors()[0];
        Class<?>[] types = constructor.getParameterTypes();
        Object[] args = new Object[types.length];
        constructor.setAccessible(true);
        return (OrderServiceImpl) constructor.newInstance(args);
    }

    private String nextOrderNumber() throws Exception {
        Method method = OrderServiceImpl.class.getDeclaredMethod("nextOrderNumber");
        method.setAccessible(true);
        return (String) method.invoke(service);
    }

    @Test
    @DisplayName("Order numbers fit the 20-character column")
    void numberFitsTheColumn() throws Exception {
        service = newService();
        String number = nextOrderNumber();

        assertEquals(PREFIX, number.substring(0, PREFIX.length()), "should stay recognisable in support chats");
        assertTrue(number.length() <= COLUMN_LENGTH,
                "order_number is VARCHAR(20) but generated '" + number + "' (" + number.length() + " chars)");
    }

    @Test
    @DisplayName("Order numbers use only unambiguous base-36 characters")
    void numberUsesSafeCharacters() throws Exception {
        service = newService();
        for (int i = 0; i < 500; i++) {
            String number = nextOrderNumber();
            assertTrue(number.matches("^#DM-[0-9A-Z]+$"), "unreadable or unsafe characters in '" + number + "'");
        }
    }

    @Test
    @DisplayName("Every number in a large batch is unique")
    void numbersDoNotCollideAtVolume() throws Exception {
        service = newService();
        // The old 6-digit scheme had 900k slots; at 10k orders that is roughly one
        // collision per 90 numbers, so 200k draws is more than enough to expose it.
        int draws = 200_000;
        Set<String> seen = new HashSet<>(draws * 2);
        for (int i = 0; i < draws; i++) {
            String number = nextOrderNumber();
            assertTrue(seen.add(number), "duplicate order number '" + number + "' after " + seen.size() + " draws");
        }
    }

    @RepeatedTest(3)
    @DisplayName("Concurrent checkouts never share a number")
    void concurrentDrawsAreUnique() throws Exception {
        service = newService();
        int threads = 16;
        int perThread = 2_000;

        ExecutorService pool = Executors.newFixedThreadPool(threads);
        // Released together, so the draws genuinely overlap. A scheme that only
        // holds when calls happen to be serial would pass a sequential test.
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<String[]>> results = new ArrayList<>();
            for (int t = 0; t < threads; t++) {
                results.add(pool.submit((Callable<String[]>) () -> {
                    start.await(30, TimeUnit.SECONDS);
                    String[] mine = new String[perThread];
                    for (int i = 0; i < perThread; i++) {
                        mine[i] = nextOrderNumber();
                    }
                    return mine;
                }));
            }
            start.countDown();

            Set<String> seen = new HashSet<>(threads * perThread * 2);
            for (Future<String[]> result : results) {
                for (String number : result.get(60, TimeUnit.SECONDS)) {
                    assertTrue(seen.add(number),
                            "duplicate order number '" + number + "' under " + threads + " concurrent checkouts");
                }
            }
            assertEquals(threads * perThread, seen.size());
        } finally {
            pool.shutdownNow();
        }
    }
}

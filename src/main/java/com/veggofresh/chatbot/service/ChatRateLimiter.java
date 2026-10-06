package com.veggofresh.chatbot.service;

import com.veggofresh.platform.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-customer sliding-window limit on chat messages (default 20 per minute).
 *
 * <p>In-memory, so the limit is per application instance. That is fine while the
 * backend runs as a single node; with several nodes behind a load balancer each
 * node enforces its own window (the effective limit becomes N x the setting).
 */
@Component
public class ChatRateLimiter {

    private static final long WINDOW_MS = 60_000L;
    private static final int PRUNE_THRESHOLD = 10_000;

    private final int maxPerMinute;
    private final ConcurrentHashMap<UUID, Deque<Long>> hits = new ConcurrentHashMap<>();

    public ChatRateLimiter(@Value("${veggofresh.chatbot.max-messages-per-minute:20}") int maxPerMinute) {
        this.maxPerMinute = Math.max(1, maxPerMinute);
    }

    /** Records one message for the customer, or throws 429 if they are over the limit. */
    public void check(UUID userId) {
        long now = System.currentTimeMillis();
        long cutoff = now - WINDOW_MS;
        Deque<Long> window = hits.computeIfAbsent(userId, k -> new ArrayDeque<>());
        synchronized (window) {
            while (!window.isEmpty() && window.peekFirst() < cutoff) {
                window.pollFirst();
            }
            if (window.size() >= maxPerMinute) {
                throw new BusinessException("CHAT_RATE_LIMITED",
                        "You're sending messages too fast. Please wait a moment and try again.",
                        HttpStatus.TOO_MANY_REQUESTS);
            }
            window.addLast(now);
        }
        if (hits.size() > PRUNE_THRESHOLD) {
            prune(cutoff);
        }
    }

    private void prune(long cutoff) {
        hits.entrySet().removeIf(e -> {
            Deque<Long> d = e.getValue();
            synchronized (d) {
                return d.isEmpty() || d.peekLast() < cutoff;
            }
        });
    }
}

package com.veggofresh.notification.config;

import java.security.Principal;

/**
 * STOMP user principal. Its {@code name} is the recipient's auth User UUID
 * (the exact same String the REST {@code JwtAuthenticationFilter} uses as its
 * principal), so {@code SimpMessagingTemplate.convertAndSendToUser(userId, ...)}
 * with {@code userId.toString()} resolves to the session that authenticated
 * with this principal.
 */
public record StompPrincipal(String name) implements Principal {

    public String getName() {
        return name;
    }
}
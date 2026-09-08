package com.veggofresh.notification.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Real-time notification transport built on Spring's built-in WebSocket +
 * STOMP support. There is deliberately NO external broker: the in-memory
 * {@code SimpleBroker} is the deferred, deadline-friendly choice (the spec's
 * "get it working before the deadline" version). Swap
 * {@code enableSimpleBroker(...)} for a Full STOMP broker relay later without
 * touching any producer/consumer code.
 *
 * <p>Both client types share the <b>{@code /ws}</b> handshake endpoint:
 * <ul>
 *   <li><b>Web</b> → SockJS client (auto-fallback) connecting to
 *       {@code /ws} — the second registration adds the SockJS transport
 *       mappings ({@code /ws/info}, {@code /ws/...}) under the same path.</li>
 *   <li><b>Flutter</b> → raw STOMP-over-WebSocket (e.g. {@code stomp_dart_client})
 *       opening {@code ws(s)://host/ws} directly — the first registration.</li>
 * </ul>
 *
 * <p>Private per-user messages flow to subscribers of
 * {@code /user/queue/notifications}; each authenticated session is scoped to
 * the user UUID principal resolved by {@link WebSocketAuthInterceptor}.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final WebSocketAuthInterceptor authInterceptor;
    private final StompWebSocketHandshakeHandler handshakeHandler;
    private final String[] allowedOriginPatterns;

    public WebSocketConfig(WebSocketAuthInterceptor authInterceptor,
                           StompWebSocketHandshakeHandler handshakeHandler,
                           @Value("${veggofresh.websocket.allowed-origin-patterns:*}") String allowedOriginPatterns) {
        this.authInterceptor = authInterceptor;
        this.handshakeHandler = handshakeHandler;
        this.allowedOriginPatterns = allowedOriginPatterns == null || allowedOriginPatterns.isBlank()
                ? new String[]{"*"}
                : java.util.Arrays.stream(allowedOriginPatterns.split(",", -1))
                        .map(String::trim).filter(s -> !s.isEmpty()).toArray(String[]::new);
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // In-memory broker, no external dependency. Private user destinations
        // (user-specific notification queues) live under /queue; public admin
        // announcements under /topic. Reserved for any client → server sends.
        registry.enableSimpleBroker("/queue", "/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Raw STOMP-over-WebSocket (Flutter stomp_dart_client).
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(allowedOriginPatterns)
                .setHandshakeHandler(handshakeHandler)
                .addInterceptors(authInterceptor);

        // SockJS fallback transport for web clients — same /ws endpoint path.
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(allowedOriginPatterns)
                .setHandshakeHandler(handshakeHandler)
                .addInterceptors(authInterceptor)
                .withSockJS();
    }
}
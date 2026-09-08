package com.veggofresh.notification.config;

import com.veggofresh.platform.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

/**
 * Authenticates the WebSocket handshake by REUSING the platform JWT utility
 * ({@link JwtTokenProvider}) — no new auth library. The handshake request is
 * permitted at the HTTP filter layer (see {@code SecurityConfig.PUBLIC_URLS}),
 * so this interceptor is the only gatekeeper for socket connections.
 *
 * <p>Token can arrive two ways, matching the two client types:
 * <ul>
 *   <li>{@code Authorization: Bearer <jwt>} header — e.g. raw STOMP clients
 *       (Flutter's {@code stomp_dart_client}) that can set headers on the
 *       underlying WS request, or</li>
 *   <li>{@code ?token=<jwt>} query parameter — required for SockJS clients
 *       (web) that cannot set a handshake header.</li>
 * </ul>
 *
 * <p>On success a {@link StompPrincipal} (name = user UUID) is parked in the
 * session attributes and later surfaced as the STOMP session Principal by
 * {@link StompWebSocketHandshakeHandler}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements HandshakeInterceptor {

    /** Attribute key under which the authenticated principal is stashed. */
    public static final String PRINCIPAL_ATTR = "veggofresh.ws.principal";

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        String token = resolveToken(request);
        if (token == null || !jwtTokenProvider.validateToken(token) || !jwtTokenProvider.isAccessToken(token)) {
            log.warn("WebSocket handshake rejected: missing/invalid/expired JWT from origin {}",
                    request.getRemoteAddress());
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        UUID userId = jwtTokenProvider.extractUserId(token);
        attributes.put(PRINCIPAL_ATTR, new StompPrincipal(userId.toString()));
        log.debug("WebSocket authenticated: user {}", userId);
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // nothing to clean up
    }

    private String resolveToken(ServerHttpRequest request) {
        String header = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7).trim();
        }

        URI uri = request.getURI();
        if (uri != null && uri.getRawQuery() != null) {
            String token = UriComponentsBuilder.fromUri(uri)
                    .build()
                    .getQueryParams()
                    .getFirst("token");
            if (token != null && !token.isBlank()) {
                return java.net.URLDecoder.decode(token, StandardCharsets.UTF_8);
            }
        }
        return null;
    }
}
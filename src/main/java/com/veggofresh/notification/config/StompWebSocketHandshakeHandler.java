package com.veggofresh.notification.config;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.security.Principal;
import java.util.Map;

/**
 * Surfaces the principal that {@link WebSocketAuthInterceptor} authenticated
 * and stashed in the session attributes as the STOMP/WebSocket Principal.
 * This is what makes {@code convertAndSendToUser(userId, ...)} route messages
 * to the right connected session.
 */
@Component
public class StompWebSocketHandshakeHandler extends DefaultHandshakeHandler {

    @Override
    protected Principal determineUser(ServerHttpRequest request, WebSocketHandler wsHandler,
                                      Map<String, Object> attributes) {
        Object principal = attributes.get(WebSocketAuthInterceptor.PRINCIPAL_ATTR);
        if (principal instanceof Principal p) {
            return p;
        }
        return super.determineUser(request, wsHandler, attributes);
    }
}
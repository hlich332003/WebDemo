package com.mycompany.myapp.config;

import java.security.Principal;
import java.util.Map;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

/**
 * Custom HandshakeHandler để gán Principal cho WebSocket Session
 * dựa trên thông tin đã được xác thực trong JwtHandshakeInterceptor.
 */
@Component
public class CustomHandshakeHandler extends DefaultHandshakeHandler {

    @Override
    protected Principal determineUser(ServerHttpRequest request, WebSocketHandler wsHandler, Map<String, Object> attributes) {
        // Lấy Principal từ attributes (đã được set trong JwtHandshakeInterceptor)
        return (Principal) attributes.get("SPRING.AUTHENTICATION");
    }
}

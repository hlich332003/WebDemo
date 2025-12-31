package com.mycompany.myapp.config;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class WebSocketHandshakeInterceptor implements HandshakeInterceptor {

    private final Logger log = LoggerFactory.getLogger(WebSocketHandshakeInterceptor.class);

    @Override
    public boolean beforeHandshake(
        ServerHttpRequest request,
        ServerHttpResponse response,
        WebSocketHandler wsHandler,
        Map<String, Object> attributes
    ) throws Exception {
        log.info("🔌 WebSocket Handshake Request: {}", request.getURI());

        // Extract guestId from query parameters and put it into the session attributes
        String path = request.getURI().getPath();
        String query = request.getURI().getQuery();

        if (query != null) {
            UriComponentsBuilder.fromUri(request.getURI())
                .build()
                .getQueryParams()
                .forEach((key, value) -> {
                    if ("guestId".equals(key) && !value.isEmpty()) {
                        attributes.put("guestId", value.get(0));
                        log.info("✅ Found guestId in query param: {}", value.get(0));
                    }
                });
        }

        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Exception exception) {
        if (exception != null) {
            log.error("❌ WebSocket Handshake Error", exception);
        } else {
            log.info("✅ WebSocket Handshake Complete: {}", request.getURI());
        }
    }
}

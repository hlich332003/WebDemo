package com.mycompany.myapp.config;

import java.security.Principal;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;

@Configuration
@Order(Ordered.HIGHEST_PRECEDENCE + 99)
public class WebSocketChannelInterceptor implements ChannelInterceptor {

    private final Logger log = LoggerFactory.getLogger(WebSocketChannelInterceptor.class);

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && accessor.getUser() == null) {
            Map<String, Object> sessionAttrs = accessor.getSessionAttributes();
            if (sessionAttrs != null) {
                // Lấy Principal từ Session Attributes (đã được set trong JwtHandshakeInterceptor)
                Object obj = sessionAttrs.get("SPRING.AUTHENTICATION");
                if (obj instanceof Principal principal) {
                    accessor.setUser(principal);
                } else {
                    log.debug("No Principal found in session attributes during preSend; SPRING.AUTHENTICATION missing or not a Principal");
                }
            } else {
                log.debug("No session attributes available in STOMP accessor during preSend");
            }
        }
        return message;
    }
}

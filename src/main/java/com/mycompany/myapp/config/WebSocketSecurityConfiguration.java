package com.mycompany.myapp.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.util.StringUtils;

/**
 * WebSocket security configuration: provides a ChannelInterceptor that authenticates
 * STOMP CONNECT frames by extracting a Bearer token from native headers (or token header)
 * and converting it to a Spring Authentication using existing JwtDecoder + JwtAuthenticationConverter.
 */
@Configuration
public class WebSocketSecurityConfiguration {

    private static final Logger log = LoggerFactory.getLogger(WebSocketSecurityConfiguration.class);

    @Bean
    public ChannelInterceptor stompAuthChannelInterceptor(JwtDecoder jwtDecoder, JwtAuthenticationConverter jwtAuthenticationConverter) {
        return new ChannelInterceptor() {
            @Override
            public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                if (accessor == null) return message;

                if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                    try {
                        String authHeader = accessor.getFirstNativeHeader("Authorization");
                        String token = null;
                        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
                            token = authHeader.substring(7);
                        } else {
                            // Fallback: SockJS handshake may pass token in the ``nativeHeaders.token`` or query param
                            String tokenHeader = accessor.getFirstNativeHeader("token");
                            if (StringUtils.hasText(tokenHeader)) {
                                token = tokenHeader;
                            }
                        }

                        if (StringUtils.hasText(token)) {
                            var jwt = jwtDecoder.decode(token);
                            Authentication auth = jwtAuthenticationConverter.convert(jwt);
                            accessor.setUser(auth);
                            SecurityContextHolder.getContext().setAuthentication(auth);
                            log.info("[WS_AUTH_OK] Principal set from STOMP CONNECT: {}", auth.getName());
                        } else {
                            log.warn("[WS_NO_TOKEN] No token provided in STOMP CONNECT headers");
                        }
                    } catch (Exception e) {
                        log.warn("[WS_AUTH_FAILED] Failed to authenticate STOMP CONNECT: {}", e.getMessage());
                    }
                }

                return message;
            }
        };
    }
}

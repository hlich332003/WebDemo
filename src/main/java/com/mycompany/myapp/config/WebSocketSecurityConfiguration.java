package com.mycompany.myapp.config;

import java.util.Collections;
import java.util.List;
import java.util.Map;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.util.StringUtils;

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

                // Handle CONNECT command
                if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                    try {
                        // 1. Try to get JWT Token
                        String authHeader = accessor.getFirstNativeHeader("Authorization");
                        String token = null;
                        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
                            token = authHeader.substring(7);
                        } else {
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
                            // 2. If no token, check for Guest ID from Session Attributes (set by HandshakeInterceptor)
                            String guestId = null;
                            Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
                            if (sessionAttributes != null && sessionAttributes.containsKey("guestId")) {
                                guestId = (String) sessionAttributes.get("guestId");
                            }

                            // Fallback: Check STOMP headers
                            if (!StringUtils.hasText(guestId)) {
                                guestId = accessor.getFirstNativeHeader("guestId");
                            }

                            if (StringUtils.hasText(guestId)) {
                                List<SimpleGrantedAuthority> authorities = Collections.singletonList(
                                    new SimpleGrantedAuthority("ROLE_GUEST")
                                );
                                Authentication guestAuth = new UsernamePasswordAuthenticationToken(guestId, null, authorities);
                                accessor.setUser(guestAuth);
                                SecurityContextHolder.getContext().setAuthentication(guestAuth);
                                log.info("[WS_GUEST_OK] Guest Principal set: {}", guestId);
                            } else {
                                log.warn("[WS_NO_TOKEN] No token or guestId provided in STOMP CONNECT headers/session");
                            }
                        }
                    } catch (Exception e) {
                        log.warn("[WS_AUTH_FAILED] Failed to authenticate STOMP CONNECT: {}", e.getMessage());
                    }
                }
                // For MESSAGE commands, ensure authentication is propagated from session
                else if (StompCommand.SEND.equals(accessor.getCommand()) || StompCommand.MESSAGE.equals(accessor.getCommand())) {
                    // If no user is set, try to get from session attributes (guest user)
                    if (accessor.getUser() == null) {
                        Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
                        if (sessionAttributes != null && sessionAttributes.containsKey("guestId")) {
                            String guestId = (String) sessionAttributes.get("guestId");
                            List<SimpleGrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_GUEST"));
                            Authentication guestAuth = new UsernamePasswordAuthenticationToken(guestId, null, authorities);
                            accessor.setUser(guestAuth);
                            SecurityContextHolder.getContext().setAuthentication(guestAuth);
                            log.debug("[WS_MESSAGE] Guest Principal restored for MESSAGE: {}", guestId);
                        }
                    }
                }

                return message;
            }
        };
    }
}

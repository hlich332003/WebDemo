package com.mycompany.myapp.config;

import com.mycompany.myapp.security.AuthoritiesConstants;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.config.annotation.web.socket.EnableWebSocketSecurity;
import org.springframework.security.messaging.access.intercept.MessageMatcherDelegatingAuthorizationManager;

@Configuration
@EnableWebSocketSecurity
public class WebSocketSecurityConfig {

    @Bean
    public AuthorizationManager<Message<?>> messageAuthorizationManager(MessageMatcherDelegatingAuthorizationManager.Builder messages) {
        return messages
            // Admin tracking endpoint
            .simpDestMatchers("/topic/tracker").hasAuthority(AuthoritiesConstants.ADMIN)

            // Admin chat endpoints - both ADMIN and CSKH can access
            .simpDestMatchers("/topic/admin/chat", "/topic/admin/messages", "/topic/admin/tickets")
                .hasAnyAuthority(AuthoritiesConstants.ADMIN, AuthoritiesConstants.CSKH)
            .simpDestMatchers("/app/chat/**")
                .hasAnyAuthority(AuthoritiesConstants.ADMIN, AuthoritiesConstants.CSKH)

            // Admin notifications
            .simpDestMatchers("/topic/notifications/admin")
                .hasAnyAuthority(AuthoritiesConstants.ADMIN, AuthoritiesConstants.CSKH)

            // User-specific subscriptions
            .simpSubscribeDestMatchers("/user/queue/**", "/user/topic/**").authenticated()

            // Application destinations
            .simpDestMatchers("/app/topic/chat/admin").authenticated()

            // Public topics (if needed)
            .simpDestMatchers("/topic/public/**").permitAll()

            // System message types
            .simpTypeMatchers(
                SimpMessageType.CONNECT,
                SimpMessageType.HEARTBEAT,
                SimpMessageType.UNSUBSCRIBE,
                SimpMessageType.DISCONNECT
            ).permitAll()

            // Default: all other messages must be authenticated
            .anyMessage().authenticated()
            .build();
    }
}

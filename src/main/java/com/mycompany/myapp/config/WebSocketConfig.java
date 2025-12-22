package com.mycompany.myapp.config;

import com.mycompany.myapp.domain.User;
import com.mycompany.myapp.repository.UserRepository;
import java.security.Principal;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadPoolExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
@Order(Ordered.HIGHEST_PRECEDENCE + 99)
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final Logger log = LoggerFactory.getLogger(WebSocketConfig.class);
    private final JwtDecoder jwtDecoder;
    private final JwtAuthenticationConverter jwtAuthenticationConverter;
    private final UserRepository userRepository;

    public WebSocketConfig(
        JwtDecoder jwtDecoder, 
        JwtAuthenticationConverter jwtAuthenticationConverter,
        UserRepository userRepository
    ) {
        this.jwtDecoder = jwtDecoder;
        this.jwtAuthenticationConverter = jwtAuthenticationConverter;
        this.userRepository = userRepository;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.setApplicationDestinationPrefixes("/app");
        config.enableSimpleBroker("/topic", "/queue")
            .setHeartbeatValue(new long[]{25000, 25000})
            .setTaskScheduler(createTaskScheduler());
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry
            .addEndpoint("/ws")
            .setAllowedOriginPatterns("*")
            .withSockJS()
            .setHeartbeatTime(25000)
            .setDisconnectDelay(5000)
            .setStreamBytesLimit(512 * 1024)
            .setHttpMessageCacheSize(1000)
            .setSessionCookieNeeded(false)
            .setClientLibraryUrl("https://cdn.jsdelivr.net/npm/sockjs-client@1/dist/sockjs.min.js");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(
            new ChannelInterceptor() {
                @Override
                public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
                    StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                    if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
                        List<String> authorization = accessor.getNativeHeader("X-Authorization");
                        log.debug("X-Authorization header: {}", authorization);

                        if (authorization != null && !authorization.isEmpty()) {
                            String bearerToken = authorization.get(0);
                            if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
                                String jwtToken = bearerToken.substring(7);
                                try {
                                    Jwt jwt = jwtDecoder.decode(jwtToken);
                                    Authentication authentication = jwtAuthenticationConverter.convert(jwt);
                                    
                                    // Lấy login từ token (thường là sub)
                                    String login = authentication.getName();
                                    
                                    // Cố gắng lấy email từ claim trước
                                    String email = jwt.getClaimAsString("email");
                                    
                                    // Nếu không có email trong claim, hoặc cần chắc chắn, tra cứu DB
                                    // Tra cứu DB để lấy email chính xác (vì user có thể đăng nhập bằng SĐT)
                                    if (email == null) {
                                         email = userRepository.findOneWithAuthoritiesByEmailOrPhone(login)
                                            .map(User::getEmail)
                                            .orElse(login);
                                    }

                                    final String finalEmail = email.toLowerCase();
                                    
                                    // Tạo một Principal mới với email làm name
                                    Principal principal = new Principal() {
                                        @Override
                                        public String getName() {
                                            return finalEmail;
                                        }
                                    };
                                    
                                    accessor.setUser(principal);
                                    log.debug("User {} (login: {}) authenticated successfully via WebSocket", principal.getName(), login);
                                } catch (Exception e) {
                                    log.error("WebSocket authentication failed: {}", e.getMessage());
                                }
                            }
                        } else {
                            log.warn("No X-Authorization header found in WebSocket CONNECT frame");
                        }
                    }
                    return message;
                }
            }
        );
    }

    @Bean
    public TaskScheduler createTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(5);
        scheduler.setThreadNamePrefix("websocket-heartbeat-");
        scheduler.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(60);
        scheduler.initialize();
        return scheduler;
    }
}

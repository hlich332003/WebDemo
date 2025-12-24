package com.mycompany.myapp.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
@Order(Ordered.HIGHEST_PRECEDENCE + 99)
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final Logger log = LoggerFactory.getLogger(WebSocketConfig.class);
    private final JwtHandshakeInterceptor jwtHandshakeInterceptor;
    private final CustomHandshakeHandler customHandshakeHandler;
    private final WebSocketChannelInterceptor webSocketChannelInterceptor;

    public WebSocketConfig(
        JwtHandshakeInterceptor jwtHandshakeInterceptor,
        CustomHandshakeHandler customHandshakeHandler,
        WebSocketChannelInterceptor webSocketChannelInterceptor
    ) {
        this.jwtHandshakeInterceptor = jwtHandshakeInterceptor;
        this.customHandshakeHandler = customHandshakeHandler;
        this.webSocketChannelInterceptor = webSocketChannelInterceptor;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.setApplicationDestinationPrefixes("/app");
        config
            .enableSimpleBroker("/topic", "/queue")
            .setHeartbeatValue(new long[] { 25000, 25000 })
            .setTaskScheduler(createTaskScheduler());
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Use canonical '/websocket' endpoint so SockJS client connects to /websocket/info
        registry
            .addEndpoint("/websocket")
            .addInterceptors(jwtHandshakeInterceptor) // 1. Validate token & set attribute
            .setHandshakeHandler(customHandshakeHandler) // 2. Set Principal cho session
            .setAllowedOriginPatterns("*")
            .withSockJS();
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(webSocketChannelInterceptor);
    }

    @Bean
    public TaskScheduler createTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("websocket-heartbeat-");
        scheduler.initialize();
        return scheduler;
    }
}

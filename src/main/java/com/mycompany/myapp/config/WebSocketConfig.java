package com.mycompany.myapp.config;

import io.undertow.websockets.jsr.WebSocketDeploymentInfo;
import org.springframework.boot.web.embedded.undertow.UndertowServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.xnio.ByteBufferSlicePool;
import org.xnio.OptionMap;
import org.xnio.Xnio;
import org.xnio.XnioWorker;

@Configuration
@EnableWebSocketMessageBroker
@Order(Ordered.HIGHEST_PRECEDENCE + 99)
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final ChannelInterceptor stompAuthChannelInterceptor;
    private final HandshakeInterceptor handshakeInterceptor;

    public WebSocketConfig(ChannelInterceptor stompAuthChannelInterceptor, HandshakeInterceptor handshakeInterceptor) {
        this.stompAuthChannelInterceptor = stompAuthChannelInterceptor;
        this.handshakeInterceptor = handshakeInterceptor;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // FIX: Enable /topic for public/group chat and /queue for user-specific messages
        config.enableSimpleBroker("/topic", "/queue").setTaskScheduler(getTaskScheduler()).setHeartbeatValue(new long[] { 10000, 10000 }); // Send heartbeat every 10 seconds
        config.setApplicationDestinationPrefixes("/app");
        // FIX: Configure user destination prefix for convertAndSendToUser
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(@NonNull StompEndpointRegistry registry) {
        // FIX: Register HandshakeInterceptor to capture guestId from query params
        registry.addEndpoint("/websocket").addInterceptors(handshakeInterceptor).setAllowedOriginPatterns("*").withSockJS();
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(stompAuthChannelInterceptor);
    }

    @Bean
    public TaskScheduler getTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(2);
        scheduler.setThreadNamePrefix("websocket-heartbeat-");
        scheduler.initialize();
        return scheduler;
    }

    @Bean
    public WebServerFactoryCustomizer<UndertowServletWebServerFactory> undertowWebSocketCustomizer() {
        return factory ->
            factory.addDeploymentInfoCustomizers(deploymentInfo -> {
                WebSocketDeploymentInfo webSocketDeploymentInfo = new WebSocketDeploymentInfo();
                webSocketDeploymentInfo.setBuffers(new ByteBufferSlicePool(1024, 10240));
                deploymentInfo.addServletContextAttribute(WebSocketDeploymentInfo.ATTRIBUTE_NAME, webSocketDeploymentInfo);
            });
    }
}

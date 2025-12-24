package com.mycompany.myapp.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
public class WebSocketCorsConfig {

    @Bean
    public FilterRegistrationBean<CorsFilter> websocketCorsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        // Important: allow credentials because SockJS/STOMP may use cookies or auth with credentials
        config.setAllowCredentials(true);
        // Allow the dev front-end origins explicitly (do not use "*")
        config.addAllowedOriginPattern("http://localhost:9001");
        config.addAllowedOriginPattern("http://localhost:4200");
        // Allow typical headers used by SockJS/STOMP and JWT
        config.addAllowedHeader("*");
        // Allow all methods used by SockJS (use wildcard to be safe)
        config.addAllowedMethod("*");
        config.setMaxAge(1800L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // Match SockJS endpoints: /websocket/info and all transports
        source.registerCorsConfiguration("/websocket/**", config);

        FilterRegistrationBean<CorsFilter> bean = new FilterRegistrationBean<>(new CorsFilter(source));
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return bean;
    }
}

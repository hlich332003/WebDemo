package com.mycompany.myapp.config;

import com.mycompany.myapp.domain.User;
import com.mycompany.myapp.repository.UserRepository;
import java.security.Principal;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

@Component
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    private final Logger log = LoggerFactory.getLogger(JwtHandshakeInterceptor.class);
    private final JwtDecoder jwtDecoder;
    private final JwtAuthenticationConverter jwtAuthenticationConverter;
    private final UserRepository userRepository;

    public JwtHandshakeInterceptor(
        JwtDecoder jwtDecoder,
        JwtAuthenticationConverter jwtAuthenticationConverter,
        UserRepository userRepository
    ) {
        this.jwtDecoder = jwtDecoder;
        this.jwtAuthenticationConverter = jwtAuthenticationConverter;
        this.userRepository = userRepository;
    }

    @Override
    public boolean beforeHandshake(
        ServerHttpRequest request,
        ServerHttpResponse response,
        WebSocketHandler wsHandler,
        Map<String, Object> attributes
    ) {
        try {
            // Log incoming request URI and query for diagnostics
            log.debug("WebSocket handshake request URI: {}", request.getURI());
            String query = request.getURI().getQuery();
            log.debug("WebSocket handshake query string: {}", query);

            String token = null;
            if (request instanceof ServletServerHttpRequest servletRequest) {
                token = servletRequest.getServletRequest().getParameter("access_token");
                if (StringUtils.hasText(token)) {
                    log.debug("Found access_token in query string");
                } else {
                    String authHeader = servletRequest.getServletRequest().getHeader("Authorization");
                    if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
                        token = authHeader.substring(7);
                        log.debug("Found Bearer token in Authorization header");
                    }
                }

                if (!StringUtils.hasText(token)) {
                    log.warn(
                        "❌ No access_token or Authorization header found in WebSocket handshake (query='{}'). Handshake rejected.",
                        query
                    );
                    return false;
                }

                try {
                    Jwt jwt = jwtDecoder.decode(token);
                    Authentication authentication = jwtAuthenticationConverter.convert(jwt);

                    String login = authentication.getName();
                    String email = jwt.getClaimAsString("email");

                    if (email == null) {
                        // If repository lookup fails, fallback to login value
                        email = userRepository.findOneWithAuthoritiesByEmailOrPhone(login).map(User::getEmail).orElse(login);
                    }

                    final String finalEmail = email != null ? email.toLowerCase() : login.toLowerCase();
                    Principal principal = () -> finalEmail;

                    // ✅ QUAN TRỌNG: Lưu Principal vào attributes để HandshakeHandler sử dụng
                    attributes.put("SPRING.AUTHENTICATION", principal);

                    log.info("✅ WebSocket authenticated user: {} (sub='{}')", principal.getName(), jwt.getSubject());
                    return true; // Handshake thành công
                } catch (Exception e) {
                    log.error("❌ WebSocket handshake failed while decoding token: {}", e.toString(), e);
                    // Return false -> handshake rejected
                    return false;
                }
            }
        } catch (Exception ex) {
            log.error("❌ Unexpected error in JwtHandshakeInterceptor.beforeHandshake: {}", ex.toString(), ex);
            return false;
        }
        return false;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Exception exception) {}
}

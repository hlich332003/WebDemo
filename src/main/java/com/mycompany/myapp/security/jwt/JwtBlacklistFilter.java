package com.mycompany.myapp.security.jwt;

import com.mycompany.myapp.service.TokenBlacklistService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Filter để kiểm tra JWT token có bị blacklist không
 * Chạy sau khi Spring Security đã validate JWT
 */
@Component
public class JwtBlacklistFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtBlacklistFilter.class);
    private final TokenBlacklistService tokenBlacklistService;

    public JwtBlacklistFilter(TokenBlacklistService tokenBlacklistService) {
        this.tokenBlacklistService = tokenBlacklistService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {
        // Extract token từ Authorization header
        String token = extractToken(request);

        if (token != null) {
            // Kiểm tra token có trong blacklist không
            if (tokenBlacklistService.isBlacklisted(token)) {
                log.warn("Blacklisted token detected from IP: {}", request.getRemoteAddr());

                // Clear security context
                SecurityContextHolder.clearContext();

                // Trả về 401 Unauthorized
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"Token has been revoked\",\"message\":\"Please login again\"}");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Extract JWT token từ Authorization header
     */
    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    /**
     * Chỉ áp dụng filter cho các authenticated requests
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Cho phép các public endpoints đi qua
        String path = request.getRequestURI();
        return path.startsWith("/api/authenticate") || path.startsWith("/api/register") || path.startsWith("/api/activate");
    }
}

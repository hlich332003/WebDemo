package com.mycompany.myapp.security.jwt;

import com.mycompany.myapp.service.TokenBlacklistService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Filter để kiểm tra JWT token có bị blacklist không
 * Chạy sau khi Spring Security đã validate JWT
 */
@Component
public class JwtBlacklistFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtBlacklistFilter.class);
    private final TokenBlacklistService tokenBlacklistService;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    private final List<String> publicEndpoints = List.of(
        "/api/authenticate",
        "/api/register",
        "/api/activate",
        "/api/public/**",
        "/api/account/reset-password/init",
        "/api/account/reset-password/finish"
    );

    public JwtBlacklistFilter(TokenBlacklistService tokenBlacklistService) {
        this.tokenBlacklistService = tokenBlacklistService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {
        String token = extractToken(request);

        if (token != null && tokenBlacklistService.isBlacklisted(token)) {
            log.warn("Blacklisted token detected from IP: {}", request.getRemoteAddr());
            SecurityContextHolder.clearContext();
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Token has been revoked\",\"message\":\"Please login again\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Bỏ qua filter này nếu request URI khớp với bất kỳ endpoint public nào
        return publicEndpoints.stream().anyMatch(p -> pathMatcher.match(p, request.getRequestURI()));
    }
}

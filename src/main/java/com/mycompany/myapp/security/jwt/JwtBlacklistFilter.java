package com.mycompany.myapp.security.jwt;

import com.mycompany.myapp.service.TokenBlacklistService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
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

        if (token != null) {
            try {
                if (tokenBlacklistService.isBlacklisted(token)) {
                    log.warn("Blacklisted token detected from IP: {} for URI: {}", request.getRemoteAddr(), request.getRequestURI());
                    SecurityContextHolder.clearContext();
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"error\":\"Token has been revoked\",\"message\":\"Please login again\"}");
                    return;
                }
            } catch (Exception ex) {
                // If Redis is unavailable or check fails, do not block public endpoints — log and continue the filter chain.
                log.error("Error while checking token blacklist: {}. Proceeding without blocking.", ex.getMessage());
            }
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
        // Use servlet path (which excludes context path) for matching so filter is robust when app has context path
        final String servletPath = request.getServletPath() == null ? request.getRequestURI() : request.getServletPath();
        final String requestUri = request.getRequestURI();
        final String pathInfo = request.getPathInfo();

        // Build a small set of path candidates to match against public endpoints to be defensive
        List<String> candidates = new ArrayList<>();
        if (servletPath != null) candidates.add(servletPath);
        if (requestUri != null) candidates.add(requestUri);
        if (pathInfo != null) candidates.add(pathInfo);

        for (String p : publicEndpoints) {
            for (String candidate : candidates) {
                if (candidate == null) continue;
                try {
                    if (pathMatcher.match(p, candidate) || candidate.startsWith(p.replace("/**", ""))) {
                        return true;
                    }
                } catch (Exception ex) {
                    if (candidate.contains(p.replace("**", ""))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}

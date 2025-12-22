package com.mycompany.myapp.web.rest.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.http.HttpMethod;

import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Interceptor demo: log request/response info and do a simple JWT check.
 * - If project already has a TokenProvider/JwtUtils bean, you can replace the simple validation
 *   with a call to that bean.
 */
@Component
public class RequestLoggingInterceptor implements HandlerInterceptor {

    private final Logger log = LoggerFactory.getLogger(RequestLoggingInterceptor.class);

    // simple pattern for forbidden chars in search query
    private static final Pattern FORBIDDEN_SEARCH = Pattern.compile("[<>;\\-]");
    private static final List<String> SKIP_PATHS = Arrays.asList("/management", "/swagger-ui", "/v3/api-docs");

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        long start = Instant.now().toEpochMilli();
        request.setAttribute("_request_start_time", start);

        String method = request.getMethod();
        String uri = request.getRequestURI();
        String query = request.getQueryString();
        log.info("Incoming request: {} {}{}", method, uri, (query != null ? "?" + query : ""));

        // Bỏ qua kiểm tra cho tất cả các yêu cầu GET
        if (HttpMethod.GET.name().equalsIgnoreCase(method)) {
            return true;
        }

        // quick skip for management or swagger endpoints
        for (String p : SKIP_PATHS) {
            if (uri.startsWith(p)) {
                return true;
            }
        }

        // 1) Validate Authorization header (simple demo)
        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            // allow public endpoints under /api/public/** if you have them
            if (uri.startsWith("/api/public")) {
                return true;
            }
            log.warn("Missing or invalid Authorization header for request {} {}", method, uri);
            sendError(response, HttpServletResponse.SC_UNAUTHORIZED, "Missing or invalid Authorization header");
            return false;
        }

        String token = auth.substring(7);
        // very simple validation: token length and basic pattern; replace with your TokenProvider
        if (!isValidDemoToken(token)) {
            log.warn("Invalid token for request {} {}", method, uri);
            sendError(response, HttpServletResponse.SC_UNAUTHORIZED, "Invalid token");
            return false;
        }

        // 2) Validate search param 'q' (if present)
        String q = request.getParameter("q");
        if (q != null) {
            if (!validateSearchParam(q)) {
                log.warn("Invalid search param q='{}' for request {} {}", q, method, uri);
                sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid search parameter");
                return false;
            }
        }

        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        // noop
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        Long start = (Long) request.getAttribute("_request_start_time");
        long end = Instant.now().toEpochMilli();
        long duration = (start != null) ? (end - start) : -1;
        int status = response.getStatus();
        log.info("Completed request: {} {} => status={} took={}ms", request.getMethod(), request.getRequestURI(), status, duration);
        if (ex != null) {
            log.error("Exception while processing request", ex);
        }
    }

    private boolean validateSearchParam(String q) {
        if (q.length() > 200) return false;
        return !FORBIDDEN_SEARCH.matcher(q).find();
    }

    private boolean isValidDemoToken(String token) {
        // Demo validator: accepts tokens with length >= 10 and only base64 url-safe chars
        if (token == null) return false;
        if (token.length() < 10) return false;
        return token.matches("[A-Za-z0-9._-]+");
    }

    private void sendError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        String body = String.format("{\"status\":%d,\"message\":\"%s\"}", status, message.replaceAll("\"", "\\\""));
        response.getWriter().write(body);
    }
}

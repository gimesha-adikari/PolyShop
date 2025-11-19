package com.polyshop.authservice.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimitService rateLimitService;
    private final ObjectMapper objectMapper;
    private final BruteForceService bruteForceService;

    private static class CachedBodyHttpServletRequest extends HttpServletRequestWrapper {
        private final byte[] cachedBody;

        CachedBodyHttpServletRequest(HttpServletRequest request, byte[] cachedBody) {
            super(request);
            this.cachedBody = cachedBody;
        }

        @Override
        public ServletInputStream getInputStream() {
            final ByteArrayInputStream bais = new ByteArrayInputStream(cachedBody);

            return new ServletInputStream() {
                @Override
                public int read() {
                    return bais.read();
                }

                @Override
                public boolean isFinished() {
                    return bais.available() == 0;
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setReadListener(ReadListener readListener) {}
            };
        }

        @Override
        public BufferedReader getReader() {
            return new BufferedReader(
                    new InputStreamReader(getInputStream(), StandardCharsets.UTF_8)
            );
        }
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            jakarta.servlet.FilterChain filterChain
    ) throws ServletException, IOException {

        String path = request.getRequestURI();

        boolean protectedPath =
                path.startsWith("/api/v1/auth/request-") ||
                        path.startsWith("/api/v1/auth/register") ||
                        path.startsWith("/api/v1/auth/login") ||
                        path.startsWith("/api/v1/auth/refresh") ||
                        path.startsWith("/api/v1/auth/request-password-reset") ||
                        path.startsWith("/api/v1/auth/confirm-password-reset") ||
                        path.startsWith("/api/v1/auth/request-email-verify") ||
                        path.startsWith("/api/v1/auth/request-phone-otp") ||
                        path.startsWith("/api/v1/auth/verify-email") ||
                        path.startsWith("/api/v1/auth/verify-phone") ||
                        path.startsWith("/api/v1/auth/restore-account");

        if (!protectedPath) {
            filterChain.doFilter(request, response);
            return;
        }

        String ip = extractClientIp(request);

        if (bruteForceService.isBanned("IP:" + ip)) {
            response.setStatus(403);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"error\":\"banned\"}");
            return;
        }

        boolean ok = rateLimitService.allow("IP:" + ip, 30, 60);
        if (!ok) {
            response.setStatus(429);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"error\":\"rate_limited\"}");
            return;
        }

        String method = request.getMethod();
        String contentType = request.getContentType();
        String email = null;
        String phone = null;
        String usernameOrEmailOrPhone = null;

        if ("POST".equalsIgnoreCase(method)
                && contentType != null
                && contentType.contains(MediaType.APPLICATION_JSON_VALUE)) {

            byte[] bodyBytes = readRequestBody(request);
            HttpServletRequest cachedRequest = new CachedBodyHttpServletRequest(request, bodyBytes);

            String json = new String(bodyBytes, StandardCharsets.UTF_8);

            if (!json.isBlank()) {
                try {
                    JsonNode node = objectMapper.readTree(json);
                    if (node != null) {
                        if (node.has("email"))
                            email = node.get("email").asText(null);
                        if (node.has("usernameOrEmailOrPhone"))
                            usernameOrEmailOrPhone = node.get("usernameOrEmailOrPhone").asText(null);
                        if (node.has("phone"))
                            phone = node.get("phone").asText(null);
                    }
                } catch (Exception ignored) {}
            }

            if (phone != null && !phone.isBlank()) {
                boolean okPhone = rateLimitService.allow("PHONE:" + phone, 5, 3600);
                if (!okPhone) {
                    response.setStatus(429);
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.getWriter().write("{\"error\":\"rate_limited_phone\"}");
                    return;
                }
            }

            if ((email == null || email.isBlank())
                    && usernameOrEmailOrPhone != null
                    && !usernameOrEmailOrPhone.isBlank()
                    && usernameOrEmailOrPhone.contains("@")) {

                email = usernameOrEmailOrPhone;
            }

            if (email != null && !email.isBlank()) {
                boolean okEmail = rateLimitService.allow("EMAIL:" + email.toLowerCase(), 5, 3600);
                if (!okEmail) {
                    response.setStatus(429);
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.getWriter().write("{\"error\":\"rate_limited_email\"}");
                    return;
                }
            }

            filterChain.doFilter(cachedRequest, response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private byte[] readRequestBody(ServletRequest request) throws IOException {
        InputStream is = request.getInputStream();
        return is.readAllBytes();
    }

    private String extractClientIp(HttpServletRequest request) {
        String xf = request.getHeader("X-Forwarded-For");
        if (xf != null && !xf.isBlank()) {
            return xf.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp;
        }
        return request.getRemoteAddr();
    }
}

package com.polyshop.authservice.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import com.polyshop.authservice.service.AuthTokenService;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final AuthTokenService authTokenService;
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final String headerName = "Authorization";
    private final String bearer = "Bearer ";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String h = request.getHeader(headerName);
        if (h != null && h.startsWith(bearer)) {
            String token = h.substring(bearer.length());
            try {
                Jws<Claims> jws = jwtUtil.parseToken(token);
                Claims body = jws.getBody();
                String subject = body.getSubject();
                String jti = body.get("jti", String.class);
                if (jti == null || !authTokenService.isAccessTokenValid(jti)) {
                    filterChain.doFilter(request, response);
                    return;
                }
                @SuppressWarnings("unchecked")
                List<String> roles = body.get("roles", List.class);
                List<SimpleGrantedAuthority> authorities = (roles == null) ? List.of() : roles.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList());
                AbstractAuthenticationToken auth = new UsernamePasswordAuthenticationToken(subject, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (Exception ex) {
                log.debug("jwt parse/auth error: {}", ex.getMessage());
            }
        }
        filterChain.doFilter(request, response);
    }
}

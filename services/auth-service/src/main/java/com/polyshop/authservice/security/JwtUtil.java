package com.polyshop.authservice.security;

import io.jsonwebtoken.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JwtUtil {

    private final KeyProvider keyProvider;

    @Value("${auth.jwt.expires-in:900}")
    private long expiresSeconds;

    @Value("${auth.jwt.issuer:polyshop-auth}")
    private String issuer;


    public String generateAccessToken(String subject, List<String> roles, String jti) {
        KeyPair kp = keyProvider.getActiveKeyPair();
        PrivateKey privateKey = kp.getPrivate();

        String kid = keyProvider.getActiveKid();
        String finalJti = jti == null ? UUID.randomUUID().toString() : jti;

        Instant now = Instant.now();
        Date issuedAt = Date.from(now);
        Date exp = Date.from(now.plusSeconds(expiresSeconds));

        return Jwts.builder()
                .setHeaderParam("kid", kid)
                .setSubject(subject)
                .setIssuer(issuer)
                .setIssuedAt(issuedAt)
                .setExpiration(exp)
                .setId(finalJti)
                .claim("roles", roles)
                .signWith(privateKey, SignatureAlgorithm.RS256)
                .compact();
    }

    public Jws<Claims> parseToken(String token) {
        MultiKeyResolver resolver = new MultiKeyResolver(keyProvider);

        return Jwts.parserBuilder()
                .setSigningKeyResolver(resolver)
                .build()
                .parseClaimsJws(token);
    }

    public String getSubject(String token) {
        return parseToken(token).getBody().getSubject();
    }

    public List<String> getRoles(String token) {
        Object roles = parseToken(token).getBody().get("roles");
        if (roles instanceof List<?>) {
            return ((List<?>) roles).stream().map(Object::toString).toList();
        }
        return List.of();
    }

    public String getJti(String token) {
        return parseToken(token).getBody().getId();
    }
}

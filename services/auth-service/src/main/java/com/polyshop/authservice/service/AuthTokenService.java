package com.polyshop.authservice.service;

import com.polyshop.authservice.domain.AuthToken;
import com.polyshop.authservice.domain.enums.TokenType;
import com.polyshop.authservice.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import java.util.List;

public interface AuthTokenService {
    String createToken(User user, TokenType type, long ttlSeconds);
    String createRefreshToken(User user, long refreshTtlSeconds);
    String createEmailVerificationToken(User user);
    String createAccountRestoreToken(User user);
    AuthToken validateToken(String token, TokenType expectedType);
    AuthToken validateAndConsume(String token, TokenType expectedType);
    void revokeToken(String token);
    void revokeAllForUser(Long userId);
    String rotateRefreshToken(String oldRefreshToken, long refreshTtlSeconds);
    Page<AuthToken> searchTokens(Specification<AuthToken> spec, Pageable pageable);
    List<AuthToken> findAllValidRefreshTokensForUser(Long userId, long nowOffsetSeconds);
    void createAccessTokenEntry(User user, String jti, long ttlSeconds);
    boolean isAccessTokenValid(String jti);
    void revokeAccessTokenByJti(String jti);
    void revokeAllAccessTokensForUser(Long userId);

}

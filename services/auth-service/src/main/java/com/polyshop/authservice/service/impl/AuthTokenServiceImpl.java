package com.polyshop.authservice.service.impl;

import com.polyshop.authservice.domain.AuthToken;
import com.polyshop.authservice.domain.enums.TokenType;
import com.polyshop.authservice.domain.User;
import com.polyshop.authservice.repository.AuthTokenRepository;
import com.polyshop.authservice.service.AuthTokenService;
import com.polyshop.authservice.spec.AuthTokenSpecs;
import com.polyshop.authservice.security.JwtUtil;
import com.polyshop.authservice.util.HashUtil;
import lombok.RequiredArgsConstructor;
import org.apache.commons.codec.binary.Hex;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
public class AuthTokenServiceImpl implements AuthTokenService {

    private final AuthTokenRepository authTokenRepository;
    private final JwtUtil jwtUtil;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${AUTH_REFRESH_EXPIRES_IN:2592000}")
    private long defaultRefreshTtlSeconds;

    @Value("${AUTH_EMAIL_TOKEN_EXPIRES_IN:3600}")
    private long emailTokenTtlSeconds;

    @Value("${AUTH_ACCOUNT_RESTORE_EXPIRES_IN:86400}")
    private long accountRestoreTtlSeconds;


    @Override
    public String createToken(User user, TokenType type, long ttlSeconds) {
        byte[] bytes = new byte[48];
        secureRandom.nextBytes(bytes);
        String token = Hex.encodeHexString(bytes);
        String tokenHash = HashUtil.sha256Hex(token);
        AuthToken t = new AuthToken();
        t.setTokenHash(tokenHash);
        t.setUser(user);
        t.setType(type);
        t.setExpiresAt(Instant.now().plusSeconds(ttlSeconds));
        t.setRevoked(false);
        authTokenRepository.save(t);
        return token;
    }

    @Override
    public String createRefreshToken(User user, long refreshTtlSeconds) {
        return createToken(user, TokenType.REFRESH, refreshTtlSeconds);
    }

    @Override
    public String createEmailVerificationToken(User user) {
        return createToken(user, TokenType.EMAIL_VERIFICATION, emailTokenTtlSeconds);
    }

    @Override
    public String createAccountRestoreToken(User user) {
        return createToken(user, TokenType.ACCOUNT_RESTORE, accountRestoreTtlSeconds);
    }

    @Override
    public AuthToken validateToken(String token, TokenType expectedType) {
        String hash = HashUtil.sha256Hex(token);
        Optional<AuthToken> opt;
        if (expectedType == null) opt = authTokenRepository.findByTokenHash(hash);
        else opt = authTokenRepository.findByTokenHashAndType(hash, expectedType);
        AuthToken t = opt.orElseThrow(() -> new IllegalArgumentException("invalid token"));
        if (t.isRevoked()) throw new IllegalArgumentException("token revoked");
        if (t.getExpiresAt().isBefore(Instant.now())) {
            t.setRevoked(true);
            authTokenRepository.save(t);
            throw new IllegalArgumentException("token expired");
        }
        return t;
    }

    @Override
    @Transactional
    public AuthToken validateAndConsume(String token, TokenType expectedType) {
        AuthToken t = validateToken(token, expectedType);
        t.setRevoked(true);
        authTokenRepository.save(t);
        return t;
    }

    @Override
    public void revokeToken(String token) {
        String hash = HashUtil.sha256Hex(token);
        Optional<AuthToken> opt = authTokenRepository.findByTokenHash(hash);
        opt.ifPresent(t -> {
            t.setRevoked(true);
            authTokenRepository.save(t);
        });
    }

    @Override
    public void revokeAllForUser(Long userId) {
        List<AuthToken> list = authTokenRepository.findAllByUserId(userId);
        for (AuthToken t : list) {
            t.setRevoked(true);
            authTokenRepository.save(t);
        }
    }

    @Override
    public String rotateRefreshToken(String oldRefreshToken, long refreshTtlSeconds) {
        AuthToken existing = validateToken(oldRefreshToken, TokenType.REFRESH);
        existing.setRevoked(true);
        authTokenRepository.save(existing);
        User user = existing.getUser();
        return createRefreshToken(user, refreshTtlSeconds);
    }

    @Override
    public Page<AuthToken> searchTokens(Specification<AuthToken> spec, Pageable pageable) {
        return authTokenRepository.findAll(spec, pageable);
    }

    @Override
    public List<AuthToken> findAllValidRefreshTokensForUser(Long userId, long nowOffsetSeconds) {
        Specification<AuthToken> spec =
                AuthTokenSpecs.hasType(TokenType.REFRESH)
                        .and(AuthTokenSpecs.belongsToUser(userId))
                        .and(AuthTokenSpecs.notRevoked())
                        .and(AuthTokenSpecs.expiresAfter(Instant.now().minusSeconds(nowOffsetSeconds)));
        return authTokenRepository.findAll(spec);
    }

    @Override
    public void createAccessTokenEntry(User user, String jti, long ttlSeconds) {
        String hash = HashUtil.sha256Hex(jti);
        AuthToken t = new AuthToken();
        t.setTokenHash(hash);
        t.setUser(user);
        t.setType(TokenType.ACCESS);
        t.setExpiresAt(Instant.now().plusSeconds(ttlSeconds));
        t.setRevoked(false);
        authTokenRepository.save(t);
    }

    @Override
    public boolean isAccessTokenValid(String jti) {
        if (jti == null) return false;
        String hash = HashUtil.sha256Hex(jti);
        Optional<AuthToken> opt = authTokenRepository.findByTokenHashAndType(hash, TokenType.ACCESS);
        if (opt.isEmpty()) return false;
        AuthToken t = opt.get();
        if (t.isRevoked()) return false;
        return t.getExpiresAt().isAfter(Instant.now());
    }

    @Override
    public void revokeAccessTokenByJti(String jti) {
        String hash = HashUtil.sha256Hex(jti);
        Optional<AuthToken> opt = authTokenRepository.findByTokenHashAndType(hash, TokenType.ACCESS);
        opt.ifPresent(t -> {
            t.setRevoked(true);
            authTokenRepository.save(t);
        });
    }

    @Override
    public void revokeAllAccessTokensForUser(Long userId) {
        List<AuthToken> list = authTokenRepository.findAllByUserId(userId);
        for (AuthToken t : list) {
            if (t.getType() == TokenType.ACCESS) {
                t.setRevoked(true);
                authTokenRepository.save(t);
            }
        }
    }
}

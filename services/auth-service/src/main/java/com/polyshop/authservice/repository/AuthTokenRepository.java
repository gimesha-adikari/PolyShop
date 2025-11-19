package com.polyshop.authservice.repository;

import com.polyshop.authservice.domain.AuthToken;
import com.polyshop.authservice.domain.enums.TokenType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.transaction.annotation.Transactional;

public interface AuthTokenRepository extends JpaRepository<AuthToken, Long>, JpaSpecificationExecutor<AuthToken> {
    Optional<AuthToken> findByTokenHashAndType(String tokenHash, TokenType type);
    Optional<AuthToken> findByTokenHash(String tokenHash);
    List<AuthToken> findAllByUserId(Long userId);
    void deleteAllByUserId(Long userId);
    @Modifying
    @Transactional
    @Query("delete from AuthToken t where (t.revoked = true or t.expiresAt < :now) and t.updatedAt < :cutoff")
    int deleteExpiredOrRevokedBefore(@Param("now") Instant now, @Param("cutoff") Instant cutoff);
}

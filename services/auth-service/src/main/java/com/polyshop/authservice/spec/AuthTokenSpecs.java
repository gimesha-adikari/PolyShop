package com.polyshop.authservice.spec;

import com.polyshop.authservice.domain.AuthToken;
import com.polyshop.authservice.domain.enums.TokenType;
import org.springframework.data.jpa.domain.Specification;
import java.time.Instant;

public class AuthTokenSpecs {
    public static Specification<AuthToken> hasType(TokenType type) {
        return SearchSpecifications.isEqual("type", type);
    }
    public static Specification<AuthToken> belongsToUser(Long userId) {
        return (root, query, builder) -> userId == null ? null : builder.equal(root.get("user").get("id"), userId);
    }
    public static Specification<AuthToken> notRevoked() {
        return (root, query, builder) -> builder.isFalse(root.get("revoked"));
    }
    public static Specification<AuthToken> expiresAfter(Instant now) {
        return (root, query, builder) -> builder.greaterThan(root.get("expiresAt"), now);
    }
    public static Specification<AuthToken> expiresBefore(Instant now) {
        return (root, query, builder) -> builder.lessThan(root.get("expiresAt"), now);
    }
}

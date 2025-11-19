package com.polyshop.authservice.security;

import io.jsonwebtoken.SigningKeyResolverAdapter;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.JwtException;

import java.security.Key;
import java.security.PublicKey;
import java.util.Map;

public class MultiKeyResolver extends SigningKeyResolverAdapter {

    private final KeyProvider keyProvider;

    public MultiKeyResolver(KeyProvider keyProvider) {
        this.keyProvider = keyProvider;
    }

    @Override
    public Key resolveSigningKey(JwsHeader header, String claims) {
        String kid = header.getKeyId();
        if (kid == null || kid.isBlank()) {
            throw new JwtException("Missing 'kid' in JWT header");
        }

        Map<String, PublicKey> keys = keyProvider.getAllPublicKeys();
        PublicKey pk = keys.get(kid);
        if (pk == null) {
            throw new JwtException("Unknown 'kid' in JWT header: " + kid);
        }
        return pk;
    }
}

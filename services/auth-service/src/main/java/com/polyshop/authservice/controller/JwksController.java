package com.polyshop.authservice.controller;

import com.polyshop.authservice.security.KeyProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.interfaces.RSAPublicKey;
import java.util.*;
import java.util.stream.Collectors;
import java.util.Base64;
import java.math.BigInteger;

@RestController
public class JwksController {

    private final KeyProvider keyProvider;

    public JwksController(KeyProvider keyProvider) {
        this.keyProvider = keyProvider;
    }

    @GetMapping("/.well-known/jwks.json")
    public Map<String, Object> jwks() {

        List<Map<String, Object>> keys = keyProvider.getAllPublicKeys()
                .entrySet()
                .stream()
                .map(entry -> {
                    String kid = entry.getKey();
                    RSAPublicKey key = (RSAPublicKey) entry.getValue();

                    byte[] nBytes = toUnsigned(key.getModulus());
                    byte[] eBytes = toUnsigned(key.getPublicExponent());

                    String n = Base64.getUrlEncoder().withoutPadding().encodeToString(nBytes);
                    String e = Base64.getUrlEncoder().withoutPadding().encodeToString(eBytes);

                    return Map.<String, Object>of(
                            "kty", "RSA",
                            "kid", kid,
                            "use", "sig",
                            "alg", "RS256",
                            "n", n,
                            "e", e
                    );
                })
                .collect(Collectors.toList());

        return Map.of("keys", keys);
    }

    private static byte[] toUnsigned(BigInteger value) {
        byte[] bytes = value.toByteArray();
        if (bytes.length > 1 && bytes[0] == 0x00) {
            return Arrays.copyOfRange(bytes, 1, bytes.length);
        }
        return bytes;
    }
}

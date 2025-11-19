package com.polyshop.authservice.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.*;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class KeyProvider {

    private final Map<String, KeyPair> keyStore = new ConcurrentHashMap<>();
    private volatile String activeKid;

    public KeyProvider(
            @Value("${auth.jwt.key-dir:}") String keyDir,
            @Value("${auth.jwt.allow-dev-keys:true}") boolean allowDevKeys
    ) {
        if (keyDir != null && !keyDir.isBlank()) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(Path.of(keyDir), "*.pem")) {
                for (Path pemFile : stream) {
                    loadKey(pemFile, keyDir);
                }
            } catch (IOException ignored) {}
        }

        if (keyStore.isEmpty()) {
            if (!allowDevKeys) {
                throw new IllegalStateException("No JWT keys found. Provide key directory.");
            }

            KeyPair kp = JwtKeyUtils.generateRsaKeyPair();
            String kid = UUID.randomUUID().toString();
            keyStore.put(kid, kp);
            activeKid = kid;
        } else {
            this.activeKid = keyStore.keySet().iterator().next();
        }
    }

    private void loadKey(Path privatePemPath, String dir) {
        try {
            String filename = privatePemPath.getFileName().toString();
            if (!filename.endsWith(".pem") || filename.endsWith(".pub.pem")) return;

            String kid = filename.replace(".pem", "");
            String privatePem = Files.readString(privatePemPath);
            Path publicPemPath = Path.of(dir, kid + ".pub.pem");
            if (!Files.exists(publicPemPath)) return;

            String publicPem = Files.readString(publicPemPath);

            PrivateKey privateKey = JwtKeyUtils.parsePrivateKeyPem(privatePem);
            PublicKey publicKey = JwtKeyUtils.parsePublicKeyPem(publicPem);

            if (privateKey != null && publicKey != null) {
                keyStore.put(kid, new KeyPair(publicKey, privateKey));
            }

        } catch (Exception ignored) {}
    }

    public KeyPair getActiveKeyPair() {
        return keyStore.get(activeKid);
    }

    public String getActiveKid() {
        return activeKid;
    }

    public Map<String, PublicKey> getAllPublicKeys() {
        Map<String, PublicKey> out = new LinkedHashMap<>();
        keyStore.forEach((kid, kp) -> out.put(kid, kp.getPublic()));
        return out;
    }

    @Scheduled(fixedDelayString = "${auth.jwt.rotate-ms:86400000}")
    public void rotate() {
        try {
            KeyPair kp = JwtKeyUtils.generateRsaKeyPair();
            String kid = UUID.randomUUID().toString();
            keyStore.put(kid, kp);
            activeKid = kid;
        } catch (Exception ignored) {}
    }
}

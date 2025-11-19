package com.polyshop.authservice.security;

import java.security.*;
import java.security.spec.*;
import java.util.Base64;

public final class JwtKeyUtils {

    private JwtKeyUtils() {}

    public static KeyPair generateRsaKeyPair() {
        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
            kpg.initialize(2048);
            return kpg.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("RSA algorithm not available", e);
        }
    }

    public static PrivateKey parsePrivateKeyPem(String pem) {
        if (pem == null || pem.isBlank()) return null;
        try {
            String cleaned = pem
                    .replaceAll("-----BEGIN (.*)-----", "")
                    .replaceAll("-----END (.*)-----", "")
                    .replaceAll("\\s", "");
            byte[] keyBytes = Base64.getDecoder().decode(cleaned);
            try {
                PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
                KeyFactory kf = KeyFactory.getInstance("RSA");
                return kf.generatePrivate(spec);
            } catch (Exception inner) {
                byte[] pkcs1 = keyBytes;
                byte[] pkcs8Header = new byte[] {
                        0x30, (byte)0x82
                };
                throw new RuntimeException("Failed to parse private key PEM (ensure PKCS#8 format)", inner);
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse private key PEM", e);
        }
    }

    public static PublicKey parsePublicKeyPem(String pem) {
        if (pem == null || pem.isBlank()) return null;
        try {
            String cleaned = pem
                    .replaceAll("-----BEGIN (.*)-----", "")
                    .replaceAll("-----END (.*)-----", "")
                    .replaceAll("\\s", "");
            byte[] keyBytes = Base64.getDecoder().decode(cleaned);
            X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            return kf.generatePublic(spec);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("RSA algorithm not available", e);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse public key PEM", e);
        }
    }
}

package com.epam.edp.demo.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Slf4j
@Component
public class JwtPublicKeyProvider {

    private final PublicKey publicKey;

    public JwtPublicKeyProvider(@Value("${jwt.public-key:}") String pemKey) {
        if (pemKey == null || pemKey.isBlank()) {
            log.warn("jwt.public-key not configured — report API will be accessible without authentication");
            this.publicKey = null;
            return;
        }
        try {
            String stripped = pemKey
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s+", "");
            byte[] decoded = Base64.getDecoder().decode(stripped);
            this.publicKey = KeyFactory.getInstance("RSA")
                    .generatePublic(new X509EncodedKeySpec(decoded));
            log.info("jwt.public-key loaded successfully");
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load JWT public key: " + e.getMessage(), e);
        }
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }

    public boolean isConfigured() {
        return publicKey != null;
    }
}

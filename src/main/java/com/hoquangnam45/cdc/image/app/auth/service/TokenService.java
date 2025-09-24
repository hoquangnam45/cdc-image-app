package com.hoquangnam45.cdc.image.app.auth.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.algorithms.Algorithm;
import com.hoquangnam45.cdc.image.app.auth.model.UserMdl;
import com.hoquangnam45.cdc.image.app.common.constant.CommonClaims;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

@Service
public class TokenService {
    private final Algorithm algorithm;

    public TokenService(@Value("${jwt.secret}") String secret) {
        this.algorithm = Algorithm.HMAC256(secret);
    }

    public String generateJwtToken(Instant now, Duration expireDuration, UserMdl user) {
        JWTCreator.Builder builder = JWT.create()
                .withIssuer("cdc-image-app")
                .withSubject(user.getId().toString())
                .withClaim(CommonClaims.USERNAME, user.getUsername());

        if (user.getEmail() != null) {
            builder = builder.withClaim(CommonClaims.EMAIL, user.getEmail());
        }
        if (user.getPhoneNumber() != null) {
            builder = builder.withClaim(CommonClaims.PHONE_NUMBER, user.getPhoneNumber());
        }
        if (expireDuration != null && expireDuration.isPositive()) {
            builder = builder.withExpiresAt(now.plus(expireDuration));
        }
        return builder
                .withIssuedAt(now)
                .sign(algorithm);
    }

    public String hashToken(String algorithm, String rawToken) {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance(algorithm);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        byte[] encodedHash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(encodedHash);
    }
}
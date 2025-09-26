package com.hoquangnam45.cdc.image.app.common.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.hoquangnam45.cdc.image.app.auth.model.UserMdl;
import com.hoquangnam45.cdc.image.app.common.constant.CommonClaims;
import com.hoquangnam45.cdc.image.app.common.constant.CommonConstant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

@Service
public class TokenService {
    private final Algorithm algorithm;

    public TokenService(@Value("${jwt.secret}") String secret) {
        this.algorithm = Algorithm.HMAC256(secret);
    }

    public String generateJwtToken(Instant now, Duration expireDuration, UserMdl user) {
        JWTCreator.Builder builder = JWT.create()
                .withJWTId(UUID.randomUUID().toString())
                .withIssuer(CommonConstant.JWT_ISSUER)
                .withSubject(user.getId().toString())
                .withClaim(CommonClaims.USERNAME, user.getUsername());

        if (user.getEmail() != null) {
            builder = builder.withClaim(CommonClaims.EMAIL, user.getEmail());
        }
        if (user.getPhoneNumber() != null) {
            builder = builder.withClaim(CommonClaims.PHONE_NUMBER, user.getPhoneNumber());
        }
        if (user.getEmailConfirm() == true) {
            builder = builder.withClaim(CommonClaims.EMAIL_CONFIRMED, true);
        }
        else {
            builder = builder.withClaim(CommonClaims.EMAIL_CONFIRMED, false);
        }
        if (user.getPhoneNumberConfirm() == true) {
            builder = builder.withClaim(CommonClaims.PHONE_NUMBER_CONFIRMED, true);
        }
        else {
            builder = builder.withClaim(CommonClaims.PHONE_NUMBER_CONFIRMED, false);
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

    public DecodedJWT validateJwtToken(String jwtToken) {
        return JWT.require(algorithm)
                .withIssuer(CommonConstant.JWT_ISSUER)
                .build()
                .verify(jwtToken);
    }
}
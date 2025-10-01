package com.hoquangnam45.cdc.image.app.common.service;

import com.google.cloud.kms.v1.AsymmetricSignRequest;
import com.google.cloud.kms.v1.AsymmetricSignResponse;
import com.google.cloud.kms.v1.CryptoKeyVersionName;
import com.google.cloud.kms.v1.Digest;
import com.google.cloud.kms.v1.KeyManagementServiceClient;
import com.google.protobuf.ByteString;
import com.hoquangnam45.cdc.image.app.common.constant.CommonClaims;
import com.hoquangnam45.cdc.image.app.common.constant.CommonConstant;
import com.hoquangnam45.cdc.image.app.common.model.UserMdl;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.crypto.impl.ECDSA;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.jwt.proc.DefaultJWTClaimsVerifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.HashSet;
import java.util.UUID;

@Service
public class TokenService {
    private PublicKey cachedPublicKeyPem = null;
    private final KeyManagementServiceClient kmsClient;
    private final CryptoKeyVersionName keyName;

    public TokenService(KeyManagementServiceClient kmsClient, @Value("${jwt.gcp.kms.project-id}") String projectId, @Value("${jwt.gcp.kms.location}") String location, @Value("${jwt.gcp.kms.keyring}") String keyRing, @Value("${jwt.gcp.kms.key}") String key, @Value("${jwt.gcp.kms.key-version}") String keyVersion) {
        this.kmsClient = kmsClient;
        this.keyName = CryptoKeyVersionName.of(projectId, location, keyRing, key, keyVersion);
    }

    public String generateJwtToken(Instant now, Duration expireDuration, UserMdl user) throws NoSuchAlgorithmException, JOSEException {
        JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.ES256)
                .type(JOSEObjectType.JWT)
                .build();

        JWTClaimsSet.Builder claimsSetBuilder = new JWTClaimsSet.Builder()
                .issuer(CommonConstant.JWT_ISSUER)
                .subject(user.getId().toString())
                .jwtID(UUID.randomUUID().toString())
                .issueTime(Date.from(now))
                .claim(CommonClaims.USERNAME, user.getUsername())
                .claim(CommonClaims.EMAIL_CONFIRMED, user.getEmailConfirm() == true)
                .claim(CommonClaims.PHONE_NUMBER_CONFIRMED, user.getPhoneNumberConfirm() == true);

        if (user.getEmail() != null) {
            claimsSetBuilder.claim(CommonClaims.EMAIL, user.getEmail());
        }
        if (user.getPhoneNumber() != null) {
            claimsSetBuilder.claim(CommonClaims.PHONE_NUMBER, user.getPhoneNumber());
        }
        if (expireDuration != null && expireDuration.isPositive()) {
            claimsSetBuilder.expirationTime(Date.from(now.plus(expireDuration)));
        }

        JWTClaimsSet claimsSet = claimsSetBuilder.build();
        SignedJWT unsignedJwt = new SignedJWT(header, claimsSet);

        byte[] signingInput = unsignedJwt.getSigningInput();

        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(signingInput);
        Digest sha256Digest = Digest.newBuilder().setSha256(ByteString.copyFrom(hash)).build();

        AsymmetricSignRequest signRequest = AsymmetricSignRequest.newBuilder()
                .setName(keyName.toString())
                .setDigest(sha256Digest)
                .build();

        AsymmetricSignResponse signResponse = kmsClient.asymmetricSign(signRequest);

        // Google KMS returns ECDSA signatures in DER format. JWS (ES256) expects R||S (JOSE) format.
        byte[] derSignature = signResponse.getSignature().toByteArray();
        byte[] joseSignature = ECDSA.transcodeSignatureToConcat(derSignature, 64); // 32-byte R + 32-byte S
        String signature = Base64.getUrlEncoder().withoutPadding().encodeToString(joseSignature);
        return new String(signingInput, StandardCharsets.UTF_8) + "." + signature;
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

    public JWTClaimsSet validateJwtToken(String jwtToken) throws Exception {
        if (cachedPublicKeyPem == null) {
            cachedPublicKeyPem = getPublicKey(kmsClient.getPublicKey(keyName.toString()).getPem());
        }
        SignedJWT signedJWT = SignedJWT.parse(jwtToken);
        boolean valid = signedJWT.verify(new ECDSAVerifier((ECPublicKey) cachedPublicKeyPem));
        if (!valid) {
            throw new Exception("JWT signature verification failed");
        }
        HashSet<String> requiredClaims = new HashSet<>(Arrays.asList("iss", "sub", "exp", "iat", "jti"));
        DefaultJWTClaimsVerifier<?> claimsVerifier = new DefaultJWTClaimsVerifier<>(
                null, // No specific audience check for this example, or new Audience("your-api")
                new JWTClaimsSet.Builder().issuer(CommonConstant.JWT_ISSUER).build(), // Must match this issuer exactly
                requiredClaims,
                null // No prohibited claims
        );
        claimsVerifier.verify(signedJWT.getJWTClaimsSet(), null);
        return signedJWT.getJWTClaimsSet();
    }

    private PublicKey getPublicKey(String pemKey) throws Exception {
        String pemKeyContent = pemKey
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");
        byte[] encodedKey = Base64.getDecoder().decode(pemKeyContent);

        // Step 3: Use a KeyFactory to generate the PublicKey object
        // Use "EC" for Elliptic Curve keys (ES256)
        KeyFactory keyFactory = KeyFactory.getInstance("EC");
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(encodedKey);

        return keyFactory.generatePublic(keySpec);
    }
}
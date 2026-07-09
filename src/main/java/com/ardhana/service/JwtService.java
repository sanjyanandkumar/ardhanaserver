package com.ardhana.service;

import java.time.Instant;
import java.util.Date;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;

/**
 * Issues and verifies the application's own JWTs, signed with a shared
 * secret (HMAC256). Replaces the old Supabase-issued JWT verification.
 */
@Service
public class JwtService {

    private static final String ISSUER = "ardhana";

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration-ms:86400000}")
    private long expirationMs;

    public String generateToken(String userId, String email) {
        Instant now = Instant.now();
        return JWT.create()
            .withIssuer(ISSUER)
            .withSubject(userId)
            .withClaim("email", email)
            .withIssuedAt(Date.from(now))
            .withExpiresAt(Date.from(now.plusMillis(expirationMs)))
            .sign(Algorithm.HMAC256(secret));
    }

    public DecodedJWT verifyToken(String token) throws JWTVerificationException {
        return JWT.require(Algorithm.HMAC256(secret))
            .withIssuer(ISSUER)
            .acceptLeeway(10)
            .build()
            .verify(token);
    }
}

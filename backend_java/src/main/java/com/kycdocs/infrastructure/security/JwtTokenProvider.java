package com.kycdocs.infrastructure.security;

import com.kycdocs.domain.user.UserId;
import com.kycdocs.domain.user.UserRole;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtTokenProvider {

    private final SecretKey secretKey;
    private final long expirationMs;
    private final String issuer;

    public JwtTokenProvider(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration}") long expirationMs,
            @Value("${app.jwt.issuer}") String issuer) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
        this.issuer = issuer;
    }

    public String generateToken(UserId userId, String email, UserRole role) {
        var now = new Date();
        var expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
            .issuer(issuer)
            .subject(userId.value().toString())
            .claim("email", email)
            .claim("role", role.getValue())
            .issuedAt(now)
            .expiration(expiry)
            .signWith(secretKey)
            .compact();
    }

    public String generateToken(String userId, String email, String role) {
        var now = new Date();
        var expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
            .issuer(issuer)
            .subject(userId)
            .claim("email", email)
            .claim("role", role)
            .issuedAt(now)
            .expiration(expiry)
            .signWith(secretKey)
            .compact();
    }

    public String generateSetupToken(String userId, String email, String fullName,
                                      String role, String totpSecret) {
        var now = new Date();
        var expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
            .issuer(issuer)
            .subject(userId)
            .claim("email", email)
            .claim("fullName", fullName)
            .claim("role", role)
            .claim("totpSecret", totpSecret)
            .claim("purpose", "SETUP")
            .issuedAt(now)
            .expiration(expiry)
            .signWith(secretKey)
            .compact();
    }

    public Claims validateToken(String token) {
        try {
            return Jwts.parser()
                .verifyWith(secretKey)
                .requireIssuer(issuer)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            throw new JwtAuthenticationException("Invalid or expired JWT token", e);
        }
    }

    public String getUserIdFromToken(String token) {
        return validateToken(token).getSubject();
    }

    public String getEmailFromToken(String token) {
        return validateToken(token).get("email", String.class);
    }

    public String getRoleFromToken(String token) {
        return validateToken(token).get("role", String.class);
    }

    public long getExpirationMs() {
        return expirationMs;
    }

    public static class JwtAuthenticationException extends RuntimeException {
        public JwtAuthenticationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}

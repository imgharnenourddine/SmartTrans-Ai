package com.smarttrans.ai.security;

import com.smarttrans.ai.config.ExternalConfig;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import javax.crypto.SecretKey;

@ApplicationScoped
public class JwtTokens {

    @Inject
    private ExternalConfig config;

    public String issue(UUID userId, String email, List<String> roles) {
        Instant now = Instant.now();
        Instant expiry = now.plusMillis(config.jwtExpirationMs());
        return Jwts.builder()
                .subject(userId.toString())
                .claim("email", email)
                .claim("roles", roles)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(secretKey())
                .compact();
    }

    public long expirationMs() {
        return config.jwtExpirationMs();
    }

    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(secretKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isValid(String token) {
        try {
            parse(token);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    private SecretKey secretKey() {
        String secret = config.jwtSecret()
                .orElseThrow(() -> new IllegalStateException("JWT_SECRET is required"));
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}

package com.dietcoach.project.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

@Component
public class JwtTokenProvider {

    private final SecretKey key;

    // access / refresh 만료 분리
    private final long accessExpirationMs;
    private final long refreshExpirationMs;

    public JwtTokenProvider(
            @Value("${jwt.secret:defaultTokenSecretKeyHere1234567890defaultTokenSecretKeyHere1234567890}") String secret,
            @Value("${jwt.access-expiration-ms:900000}") long accessExpirationMs, // 기본 15분
            @Value("${jwt.refresh-expiration-ms:1209600000}") long refreshExpirationMs // 기본 14일
    ) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessExpirationMs = accessExpirationMs;
        this.refreshExpirationMs = refreshExpirationMs;
    }

    // (호환용) 기존 createToken => access로 취급
    public String createToken(Long userId) {
        return createAccessToken(userId);
    }

    public String createAccessToken(Long userId) {
        return createTokenInternal(userId, "access", accessExpirationMs);
    }

    public String createRefreshToken(Long userId) {
        return createTokenInternal(userId, "refresh", refreshExpirationMs);
    }

    private String createTokenInternal(Long userId, String tokenType, long expMs) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + expMs);

        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .claim("token_type", tokenType) // ✅ 핵심
                .setIssuedAt(now)
                .setExpiration(exp)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public Long getUserId(String token) {
        Claims claims = parseClaims(token);
        return Long.valueOf(claims.getSubject());
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public boolean isRefreshToken(String token) {
        Claims claims = parseClaims(token);
        Object type = claims.get("token_type");
        return "refresh".equals(String.valueOf(type));
    }

    public LocalDateTime getExpiry(String token) {
        Claims claims = parseClaims(token);
        Date exp = claims.getExpiration();
        Instant instant = exp.toInstant();
        return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
    }

    private Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}

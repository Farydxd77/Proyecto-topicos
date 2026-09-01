package com.cuentasclaras.backend.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.Test;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

class JwtUtilTest {

    private static final String SECRET = "test-secret-1234567890-1234567890-1234567890-abcd";
    private static final long EXPIRATION_MS = 86_400_000L; // 24 h

    private final JwtUtil jwtUtil = new JwtUtil(SECRET, EXPIRATION_MS);

    private SecretKey key() {
        return Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void generateToken_setsUsernameAsSubject() {
        String token = jwtUtil.generateToken("alice");

        assertThat(jwtUtil.getUsername(token)).isEqualTo("alice");
    }

    @Test
    void generateToken_expiresIn24Hours() {
        long before = System.currentTimeMillis();
        String token = jwtUtil.generateToken("alice");
        long after = System.currentTimeMillis();

        Date expiration = Jwts.parser()
                .verifyWith(key())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getExpiration();

        assertThat(expiration.getTime())
                .isBetween(before + EXPIRATION_MS - 1000, after + EXPIRATION_MS + 1000);
    }

    @Test
    void validateToken_trueForFreshToken() {
        String token = jwtUtil.generateToken("alice");

        assertThat(jwtUtil.validateToken(token)).isTrue();
    }

    @Test
    void validateToken_falseForTamperedToken() {
        String token = jwtUtil.generateToken("alice");
        String tampered = token.substring(0, token.length() - 3)
                + (token.endsWith("aaa") ? "bbb" : "aaa");

        assertThat(jwtUtil.validateToken(tampered)).isFalse();
    }

    @Test
    void validateToken_falseForMalformedToken() {
        assertThat(jwtUtil.validateToken("not-a-jwt")).isFalse();
    }

    @Test
    void validateToken_falseForExpiredToken() {
        // Un JwtUtil con expiración negativa produce un token ya vencido.
        JwtUtil expiredIssuer = new JwtUtil(SECRET, -1000L);
        String expired = expiredIssuer.generateToken("alice");

        assertThat(jwtUtil.validateToken(expired)).isFalse();
    }
}

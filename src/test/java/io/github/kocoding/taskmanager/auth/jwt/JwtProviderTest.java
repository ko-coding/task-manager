package io.github.kocoding.taskmanager.auth.jwt;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

public class JwtProviderTest {

    private static final String SECRET = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";

    @Test
    void generateToken_subjectがuserIdになる() {
        JwtProvider jwtProvider = new JwtProvider(SECRET, 60);
        String token = jwtProvider.generateToken(1L);
        Claims claims = parse(token);
        assertEquals("1", claims.getSubject());
    }

    @Test
    void generateToken_有効期限が発行の60分後() {
        JwtProvider jwtProvider = new JwtProvider(SECRET, 60);
        String token = jwtProvider.generateToken(1L);
        Claims claims = parse(token);
        assertEquals(Duration.ofMinutes(60),
                Duration.between(claims.getIssuedAt().toInstant(), claims.getExpiration().toInstant()));
    }

    private Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET)))
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

}

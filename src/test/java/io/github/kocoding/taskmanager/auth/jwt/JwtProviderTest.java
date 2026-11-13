package io.github.kocoding.taskmanager.auth.jwt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;

public class JwtProviderTest {

    private static final String SECRET = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";
    private static final String ANOTHER_SECRET = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWc=";

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

    @Test
    void parseUserId_生成したトークンが元のuserIdに戻る() {
        JwtProvider jwtProvider = new JwtProvider(SECRET, 60);
        String token = jwtProvider.generateToken(1L);
        Long userId = jwtProvider.parseUserId(token);
        assertEquals(1L, userId);
    }

    @Test
    void parseUserId_別の鍵で生成したトークンは読めず例外になる() {
        JwtProvider jwtProvider = new JwtProvider(SECRET, 60);
        JwtProvider anotherJwtProvider = new JwtProvider(ANOTHER_SECRET, 60);
        String token = anotherJwtProvider.generateToken(1L);
        assertThrows(SignatureException.class, () -> jwtProvider.parseUserId(token));
    }

    @Test
    void parseUserId_期限切れのトークンは例外になる() {
        JwtProvider jwtProvider = new JwtProvider(SECRET, -1);
        String token = jwtProvider.generateToken(1L);
        assertThrows(ExpiredJwtException.class, () -> jwtProvider.parseUserId(token));
    }

    private Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET)))
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

}

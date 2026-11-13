package io.github.kocoding.taskmanager.auth.jwt;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtProvider {

    private final SecretKey secretKey;
    private final long expirationMinutes;

    public JwtProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration-minutes}") long expirationMinutes) {
        this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        this.expirationMinutes = expirationMinutes;
    }

    public String generateToken(Long userId) {
        // 1. 現在時刻を Instant.now() で一度だけ取る
        Instant now = Instant.now();

        // 2. 有効期限(現在時刻 + expirationMinutes 分)を計算する
        Instant expiration = now.plus(Duration.ofMinutes(expirationMinutes));

        // 3. jjwt の Jwts.builder() でトークンを組み立てる
        // 4. compact() で文字列にして返す
        return Jwts.builder()
                .subject(String.valueOf(userId)) // 誰のトークンか
                .issuedAt(Date.from(now)) // 発行時刻
                .expiration(Date.from(expiration)) // 有効期限
                .signWith(secretKey)// 署名に使う鍵
                .compact(); // 文字列にして返す
    }

    public Long parseUserId(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return Long.valueOf(claims.getSubject());
    }

}

package io.github.kocoding.taskmanager.auth.jwt;

import java.io.IOException;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;

    public JwtAuthenticationFilter(JwtProvider jwtProvider) {
        this.jwtProvider = jwtProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        // request.getHeader("Authorization") を読む
        String header = request.getHeader("Authorization");

        // null か、"Bearer " で始まらなければ、何も登録せず次へ進めて終了
        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // "Bearer " の後ろ(7文字目以降)をトークンとして取り出す
        String token = header.substring(7);

        try {
            // jwtProvider.parseUserId(token) で userId を得る
            Long userId = jwtProvider.parseUserId(token);

            // 認証情報を作って SecurityContextHolder に登録する
            Authentication authentication = new UsernamePasswordAuthenticationToken(userId, null, List.of());
            SecurityContextHolder.getContext().setAuthentication(authentication);

        } catch (JwtException | IllegalArgumentException e) {
            // 例外(JwtException と IllegalArgumentException)が出たら、何も登録しない
            // トークンが不正なら認証なしとして扱う(401はSecurity側で返す)
        }

        filterChain.doFilter(request, response);
    }

}

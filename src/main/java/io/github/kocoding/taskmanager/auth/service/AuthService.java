package io.github.kocoding.taskmanager.auth.service;

import java.util.Locale;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import io.github.kocoding.taskmanager.auth.dto.LoginRequest;
import io.github.kocoding.taskmanager.auth.dto.LoginResponse;
import io.github.kocoding.taskmanager.auth.jwt.JwtProvider;
import io.github.kocoding.taskmanager.common.exception.UnauthorizedException;
import io.github.kocoding.taskmanager.user.entity.User;
import io.github.kocoding.taskmanager.user.repository.UserRepository;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtProvider jwtProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtProvider = jwtProvider;
    }

    public User authenticate(LoginRequest request) {
        String lowerCaseEmail = request.email().toLowerCase(Locale.ROOT);
        String message = "メールアドレスまたはパスワードに誤りがあります";
        User user = userRepository.findByEmail(lowerCaseEmail)
                .orElseThrow(() -> new UnauthorizedException(message));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new UnauthorizedException(message);
        }
        return user;
    }

    public LoginResponse login(LoginRequest request) {
        User user = authenticate(request);
        String accessToken = jwtProvider.generateToken(user.getId());
        return LoginResponse.bearer(accessToken);
    }

}

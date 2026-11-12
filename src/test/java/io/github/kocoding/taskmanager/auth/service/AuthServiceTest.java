package io.github.kocoding.taskmanager.auth.service;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import io.github.kocoding.taskmanager.auth.dto.LoginRequest;
import io.github.kocoding.taskmanager.common.exception.UnauthorizedException;
import io.github.kocoding.taskmanager.user.entity.User;
import io.github.kocoding.taskmanager.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    @Test
    void authenticate_メールアドレスが存在しない場合は例外() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());
        LoginRequest request = new LoginRequest("test@example.com", "testpassword");
        assertThrows(UnauthorizedException.class, () -> authService.authenticate(request));
        verify(passwordEncoder, never()).matches(any(), any());
    }

    @Test
    void authenticate_パスワードが不一致の場合は例外() {
        LoginRequest request = new LoginRequest("test@example.com", "testpassword");
        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(new User("test", "test@example.com", "hashed")));
        when(passwordEncoder.matches("testpassword", "hashed"))
                .thenReturn(false);
        assertThrows(UnauthorizedException.class, () -> authService.authenticate(request));
    }

    @Test
    void authenticate_成功時Userが返る() {
        LoginRequest request = new LoginRequest("test@example.com", "testpassword");
        User expectedUser = new User("test", "test@example.com", "hashed");
        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(expectedUser));
        when(passwordEncoder.matches("testpassword", "hashed"))
                .thenReturn(true);
        User actualUser = authService.authenticate(request);
        assertSame(expectedUser, actualUser);
    }

    @Test
    void authenticate_大文字のメールアドレスでも小文字で検索される() {
        LoginRequest request = new LoginRequest("Test@Example.com", "testpassword");
        User expectedUser = new User("test", "test@example.com", "hashed");
        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(expectedUser));
        when(passwordEncoder.matches("testpassword", "hashed"))
                .thenReturn(true);
        User actualUser = authService.authenticate(request);
        assertSame(expectedUser, actualUser);
    }

}

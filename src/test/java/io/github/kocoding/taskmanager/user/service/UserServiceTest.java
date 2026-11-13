package io.github.kocoding.taskmanager.user.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import javax.security.sasl.AuthenticationException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import io.github.kocoding.taskmanager.common.exception.ConflictException;
import io.github.kocoding.taskmanager.common.exception.UnauthorizedException;
import io.github.kocoding.taskmanager.user.dto.CreateUserRequest;
import io.github.kocoding.taskmanager.user.dto.UserResponse;
import io.github.kocoding.taskmanager.user.entity.User;
import io.github.kocoding.taskmanager.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    void save_メールアドレスが重複していたら例外() {
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);
        CreateUserRequest request = new CreateUserRequest("testuser", "test@example.com", "testpassword");
        assertThrows(ConflictException.class, () -> userService.save(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    void save_メールアドレスは小文字化パスワードはハッシュ化されて保存() {
        when(passwordEncoder.encode("testpassword")).thenReturn("hashedpassword");
        when(userRepository.save(any())).thenReturn(new User("testuser", "test@example.com", "hashedpassword"));
        CreateUserRequest request = new CreateUserRequest("testuser", "Test@Example.com", "testpassword");
        userService.save(request);
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertEquals("test@example.com", saved.getEmail());
        assertEquals("hashedpassword", saved.getPasswordHash());
    }

    @Test
    void findById_存在するIDの場合はそのUserを返す() {
        User expectedUser = new User("test", "test@example.com", "hashedpassword");
        ReflectionTestUtils.setField(expectedUser, "id", 1L);
        when(userRepository.findById(1L))
                .thenReturn(Optional.of(expectedUser));
        UserResponse response = userService.findById(1L);
        assertEquals(1L, response.id());
        assertEquals("test", response.name());
        assertEquals("test@example.com", response.email());
    }

    @Test
    void findById_存在しないIDの場合は例外() {
        when(userRepository.findById(1L))
                .thenReturn(Optional.empty());
        assertThrows(UnauthorizedException.class, () -> userService.findById(1L));
    }

}

package io.github.kocoding.taskmanager.user.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import io.github.kocoding.taskmanager.user.dto.CreateUserRequest;
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
        // TODO: 独自例外に置き換える
        assertThrows(RuntimeException.class, () -> userService.save(request));
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

}

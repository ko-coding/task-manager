package io.github.kocoding.taskmanager.user.service;

import java.util.Locale;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import io.github.kocoding.taskmanager.common.exception.ConflictException;
import io.github.kocoding.taskmanager.user.dto.CreateUserRequest;
import io.github.kocoding.taskmanager.user.dto.UserResponse;
import io.github.kocoding.taskmanager.user.entity.User;
import io.github.kocoding.taskmanager.user.repository.UserRepository;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // ユーザー作成
    public UserResponse save(CreateUserRequest request) {
        String lowerCaseEmail = request.email().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmail(lowerCaseEmail)) {
            throw new ConflictException("このメールアドレスは既に登録されています");
        }
        User user = new User(request.name(), lowerCaseEmail, passwordEncoder.encode(request.password()));
        return UserResponse.from(userRepository.save(user));
    }

}

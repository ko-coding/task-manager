package io.github.kocoding.taskmanager.user.dto;

public record CreateUserRequest(
        String name,
        String email,
        String password) {
}

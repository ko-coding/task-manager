package io.github.kocoding.taskmanager.common.response;

public record ErrorResponse(
        int status,
        String message) {
}

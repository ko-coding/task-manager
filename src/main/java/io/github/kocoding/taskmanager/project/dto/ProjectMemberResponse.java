package io.github.kocoding.taskmanager.project.dto;

import java.time.Instant;

public record ProjectMemberResponse(
        Long userId,
        String name,
        Instant joinedAt) {
}

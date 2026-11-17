package io.github.kocoding.taskmanager.team.dto;

import java.time.Instant;

import io.github.kocoding.taskmanager.team.entity.TeamRole;

public record TeamMemberResponse(
        Long userId,
        String name,
        TeamRole role,
        Instant joinedAt) {
}

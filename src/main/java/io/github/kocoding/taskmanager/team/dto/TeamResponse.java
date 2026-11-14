package io.github.kocoding.taskmanager.team.dto;

import java.time.Instant;

import io.github.kocoding.taskmanager.team.entity.Team;
import io.github.kocoding.taskmanager.team.entity.TeamRole;

public record TeamResponse(
        Long id,
        String name,
        String description,
        TeamRole role,
        Instant createdAt) {

    public static TeamResponse from(Team team, TeamRole role) {
        return new TeamResponse(
                team.getId(), team.getName(), team.getDescription(), role, team.getCreatedAt());
    }

}

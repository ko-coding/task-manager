package io.github.kocoding.taskmanager.project.dto;

import java.time.Instant;

import io.github.kocoding.taskmanager.project.entity.Project;

public record ProjectResponse(
        Long id,
        Long teamId,
        String name,
        String description,
        Long createdBy,
        Instant createdAt) {

    public static ProjectResponse from(Project project) {
        return new ProjectResponse(project.getId(), project.getTeamId(), project.getName(), project.getDescription(),
                project.getCreatedBy(), project.getCreatedAt());
    }

}

package io.github.kocoding.taskmanager.project.dto;

import jakarta.validation.constraints.NotNull;

public record AddProjectMemberRequest(
        @NotNull Long userId) {
}

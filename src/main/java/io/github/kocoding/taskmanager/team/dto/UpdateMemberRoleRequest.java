package io.github.kocoding.taskmanager.team.dto;

import io.github.kocoding.taskmanager.team.entity.TeamRole;
import jakarta.validation.constraints.NotNull;

public record UpdateMemberRoleRequest(
        @NotNull TeamRole role) {
}

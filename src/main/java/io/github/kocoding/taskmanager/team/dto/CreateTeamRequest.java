package io.github.kocoding.taskmanager.team.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTeamRequest(
        @NotBlank @Size(max = 128) String name,
        @Size(max = 255) String description) {
}

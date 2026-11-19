package io.github.kocoding.taskmanager.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateProjectRequest(
        @NotBlank @Size(max = 128) String name,
        @Size(max = 255) String description) {
}

package io.github.kocoding.taskmanager.task.dto;

import io.github.kocoding.taskmanager.task.entity.TaskStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateTaskStatusRequest(
        @NotNull TaskStatus status) {
}

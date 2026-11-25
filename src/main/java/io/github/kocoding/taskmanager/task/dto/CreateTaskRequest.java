package io.github.kocoding.taskmanager.task.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTaskRequest(
        @NotBlank @Size(max = 128) String title,
        @Size(max = 255) String description,
        Long assignedTo,
        LocalDate dueDate) {
}

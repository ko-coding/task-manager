package io.github.kocoding.taskmanager.task.dto;

import java.time.Instant;
import java.time.LocalDate;

import io.github.kocoding.taskmanager.task.entity.Task;
import io.github.kocoding.taskmanager.task.entity.TaskStatus;

public record TaskResponse(
        Long id,
        Long projectId,
        Long assignedTo,
        Long createdBy,
        String title,
        String description,
        TaskStatus status,
        LocalDate dueDate,
        Instant createdAt,
        Instant updatedAt) {

    public static TaskResponse from(Task task) {
        return new TaskResponse(task.getId(), task.getProjectId(), task.getAssignedTo(), task.getCreatedBy(),
                task.getTitle(), task.getDescription(), task.getStatus(), task.getDueDate(), task.getCreatedAt(),
                task.getUpdatedAt());
    }

}

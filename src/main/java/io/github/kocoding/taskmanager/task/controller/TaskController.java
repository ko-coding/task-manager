package io.github.kocoding.taskmanager.task.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import io.github.kocoding.taskmanager.task.dto.CreateTaskRequest;
import io.github.kocoding.taskmanager.task.dto.TaskResponse;
import io.github.kocoding.taskmanager.task.service.TaskService;
import jakarta.validation.Valid;

@RestController
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping("/api/projects/{projectId}/tasks")
    public ResponseEntity<TaskResponse> createTask(@AuthenticationPrincipal Long userId, @PathVariable Long projectId,
            @Valid @RequestBody CreateTaskRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(taskService.create(userId, projectId, request));
    }

    @GetMapping("/api/tasks/{taskId}")
    public ResponseEntity<TaskResponse> getTask(@AuthenticationPrincipal Long userId, @PathVariable Long taskId) {
        return ResponseEntity.ok(taskService.findTask(userId, taskId));
    }

}

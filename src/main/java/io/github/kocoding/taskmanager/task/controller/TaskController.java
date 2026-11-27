package io.github.kocoding.taskmanager.task.controller;

import java.time.LocalDate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.github.kocoding.taskmanager.task.dto.CreateTaskRequest;
import io.github.kocoding.taskmanager.task.dto.TaskResponse;
import io.github.kocoding.taskmanager.task.dto.UpdateTaskRequest;
import io.github.kocoding.taskmanager.task.dto.UpdateTaskStatusRequest;
import io.github.kocoding.taskmanager.task.entity.TaskStatus;
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

    @GetMapping("/api/projects/{projectId}/tasks")
    public PagedModel<TaskResponse> getTasks(@AuthenticationPrincipal Long userId, @PathVariable Long projectId,
            @RequestParam(required = false) TaskStatus status, @RequestParam(required = false) Long assignedTo,
            @RequestParam(required = false) String keyword, @RequestParam(required = false) LocalDate dueDateFrom,
            @RequestParam(required = false) LocalDate dueDateTo, Pageable pageable) {
        Page<TaskResponse> page = taskService.findTasks(userId, projectId, status, assignedTo, keyword, dueDateFrom,
                dueDateTo, pageable);
        return new PagedModel<>(page);
    }

    @PatchMapping("/api/tasks/{taskId}")
    public ResponseEntity<TaskResponse> updateTask(@AuthenticationPrincipal Long userId, @PathVariable Long taskId,
            @Valid @RequestBody UpdateTaskRequest request) {
        return ResponseEntity.ok(taskService.updateTask(userId, taskId, request));
    }

    @PatchMapping("/api/tasks/{taskId}/status")
    public ResponseEntity<TaskResponse> updateTaskStatus(@AuthenticationPrincipal Long userId,
            @PathVariable Long taskId, @Valid @RequestBody UpdateTaskStatusRequest request) {
        return ResponseEntity.ok(taskService.updateTaskStatus(userId, taskId, request));
    }

    @DeleteMapping("/api/tasks/{taskId}")
    public ResponseEntity<Void> deleteTask(@AuthenticationPrincipal Long userId, @PathVariable Long taskId) {
        taskService.deleteTask(userId, taskId);
        return ResponseEntity.noContent().build();
    }

}

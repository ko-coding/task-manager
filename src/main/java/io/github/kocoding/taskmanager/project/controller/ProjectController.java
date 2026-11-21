package io.github.kocoding.taskmanager.project.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import io.github.kocoding.taskmanager.project.dto.CreateProjectRequest;
import io.github.kocoding.taskmanager.project.dto.ProjectResponse;
import io.github.kocoding.taskmanager.project.dto.UpdateProjectRequest;
import io.github.kocoding.taskmanager.project.service.ProjectService;
import jakarta.validation.Valid;

@RestController
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @PostMapping("/api/teams/{teamId}/projects")
    public ResponseEntity<ProjectResponse> createProject(@AuthenticationPrincipal Long userId,
            @PathVariable Long teamId, @Valid @RequestBody CreateProjectRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(projectService.create(userId, teamId, request));
    }

    @GetMapping("/api/projects/{projectId}")
    public ResponseEntity<ProjectResponse> findProject(@AuthenticationPrincipal Long userId,
            @PathVariable Long projectId) {
        return ResponseEntity.ok(projectService.findProject(userId, projectId));
    }

    @PatchMapping("/api/projects/{projectId}")
    public ResponseEntity<ProjectResponse> updateProject(@AuthenticationPrincipal Long userId,
            @PathVariable Long projectId, @Valid @RequestBody UpdateProjectRequest request) {
        return ResponseEntity.ok(projectService.updateProject(userId, projectId, request));
    }

}

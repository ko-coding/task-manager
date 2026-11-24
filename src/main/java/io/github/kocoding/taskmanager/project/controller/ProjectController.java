package io.github.kocoding.taskmanager.project.controller;

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

import io.github.kocoding.taskmanager.project.dto.AddProjectMemberRequest;
import io.github.kocoding.taskmanager.project.dto.CreateProjectRequest;
import io.github.kocoding.taskmanager.project.dto.ProjectMemberResponse;
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

    @GetMapping("/api/projects")
    public PagedModel<ProjectResponse> getProjects(@AuthenticationPrincipal Long userId,
            @RequestParam(required = false) String keyword, Pageable pageable) {
        Page<ProjectResponse> page = projectService.findMyProjects(userId, keyword, pageable);
        return new PagedModel<>(page);
    }

    @DeleteMapping("/api/projects/{projectId}")
    public ResponseEntity<Void> deleteProject(@AuthenticationPrincipal Long userId, @PathVariable Long projectId) {
        projectService.deleteProject(userId, projectId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/projects/{projectId}/members")
    public PagedModel<ProjectMemberResponse> getMembers(@AuthenticationPrincipal Long userId,
            @PathVariable Long projectId, Pageable pageable) {
        Page<ProjectMemberResponse> page = projectService.findMembers(userId, projectId, pageable);
        return new PagedModel<>(page);
    }

    @PostMapping("/api/projects/{projectId}/members")
    public ResponseEntity<ProjectMemberResponse> addProjectMember(@AuthenticationPrincipal Long userId,
            @PathVariable Long projectId, @Valid @RequestBody AddProjectMemberRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(projectService.addMember(userId, projectId, request));
    }

}

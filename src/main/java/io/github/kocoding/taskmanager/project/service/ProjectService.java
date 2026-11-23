package io.github.kocoding.taskmanager.project.service;

import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.kocoding.taskmanager.common.exception.BadRequestException;
import io.github.kocoding.taskmanager.common.exception.ForbiddenException;
import io.github.kocoding.taskmanager.common.exception.NotFoundException;
import io.github.kocoding.taskmanager.common.query.QueryUtils;
import io.github.kocoding.taskmanager.project.dto.CreateProjectRequest;
import io.github.kocoding.taskmanager.project.dto.ProjectResponse;
import io.github.kocoding.taskmanager.project.dto.UpdateProjectRequest;
import io.github.kocoding.taskmanager.project.entity.Project;
import io.github.kocoding.taskmanager.project.entity.ProjectUser;
import io.github.kocoding.taskmanager.project.entity.ProjectUserId;
import io.github.kocoding.taskmanager.project.repository.ProjectRepository;
import io.github.kocoding.taskmanager.project.repository.ProjectUserRepository;
import io.github.kocoding.taskmanager.team.entity.TeamUserId;
import io.github.kocoding.taskmanager.team.repository.TeamUserRepository;

@Service
public class ProjectService {

    private static final Map<String, String> PROJECT_SORT_ALLOWLIST = Map.of("name", "p.name", "createdAt",
            "p.createdAt");

    private final ProjectUserRepository projectUserRepository;
    private final ProjectRepository projectRepository;
    private final TeamUserRepository teamUserRepository;

    public ProjectService(TeamUserRepository teamUserRepository, ProjectRepository projectRepository,
            ProjectUserRepository projectUserRepository) {
        this.teamUserRepository = teamUserRepository;
        this.projectRepository = projectRepository;
        this.projectUserRepository = projectUserRepository;
    }

    @Transactional
    public ProjectResponse create(Long userId, Long teamId, CreateProjectRequest request) {
        if (!teamUserRepository.existsById(new TeamUserId(teamId, userId))) {
            throw new NotFoundException("チームが存在しません");
        }
        Project project = projectRepository.save(new Project(teamId, userId, request.name(), request.description()));
        projectUserRepository.save(new ProjectUser(userId, project.getId(), teamId));
        return ProjectResponse.from(project);
    }

    @Transactional(readOnly = true)
    public ProjectResponse findProject(Long userId, Long projectId) {
        String message = "プロジェクトが存在しません";
        if (!projectUserRepository.existsById(new ProjectUserId(userId, projectId))) {
            throw new NotFoundException(message);
        }
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new NotFoundException(message));
        return ProjectResponse.from(project);
    }

    @Transactional
    public ProjectResponse updateProject(Long userId, Long projectId, UpdateProjectRequest request) {
        if (!projectUserRepository.existsById(new ProjectUserId(userId, projectId))) {
            throw new NotFoundException("プロジェクトが存在しません");
        }

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new NotFoundException("プロジェクトが存在しません"));

        if (!userId.equals(project.getCreatedBy())) {
            throw new ForbiddenException("プロジェクトを更新する権限がありません");
        }

        if (request.getName() != null && request.getName().isEmpty()) {
            throw new BadRequestException("プロジェクト名にnullは指定できません");
        }

        if (request.getName() != null) {
            project.rename(request.getName().get());
        }

        if (request.getDescription() != null) {
            project.changeDescription(request.getDescription().orElse(null));
        }

        return ProjectResponse.from(project);
    }

    @Transactional(readOnly = true)
    public Page<ProjectResponse> findMyProjects(Long userId, String keyword, Pageable pageable) {
        String pattern = QueryUtils.toLikePattern(keyword);
        Pageable safePageable = QueryUtils.toSafePageable(pageable, PROJECT_SORT_ALLOWLIST, "p.name", "p.id");
        return projectUserRepository.findProjects(userId, pattern, safePageable);
    }

}

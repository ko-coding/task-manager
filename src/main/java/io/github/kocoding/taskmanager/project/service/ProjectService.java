package io.github.kocoding.taskmanager.project.service;

import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.kocoding.taskmanager.common.exception.BadRequestException;
import io.github.kocoding.taskmanager.common.exception.ConflictException;
import io.github.kocoding.taskmanager.common.exception.ForbiddenException;
import io.github.kocoding.taskmanager.common.exception.NotFoundException;
import io.github.kocoding.taskmanager.common.exception.UnprocessableContentException;
import io.github.kocoding.taskmanager.common.query.QueryUtils;
import io.github.kocoding.taskmanager.project.dto.AddProjectMemberRequest;
import io.github.kocoding.taskmanager.project.dto.CreateProjectRequest;
import io.github.kocoding.taskmanager.project.dto.ProjectMemberResponse;
import io.github.kocoding.taskmanager.project.dto.ProjectResponse;
import io.github.kocoding.taskmanager.project.dto.UpdateProjectRequest;
import io.github.kocoding.taskmanager.project.entity.Project;
import io.github.kocoding.taskmanager.project.entity.ProjectUser;
import io.github.kocoding.taskmanager.project.entity.ProjectUserId;
import io.github.kocoding.taskmanager.project.repository.ProjectRepository;
import io.github.kocoding.taskmanager.project.repository.ProjectUserRepository;
import io.github.kocoding.taskmanager.task.repository.TaskRepository;
import io.github.kocoding.taskmanager.team.entity.TeamUserId;
import io.github.kocoding.taskmanager.team.repository.TeamUserRepository;
import io.github.kocoding.taskmanager.user.entity.User;
import io.github.kocoding.taskmanager.user.repository.UserRepository;

@Service
public class ProjectService {

    private static final Map<String, String> PROJECT_SORT_ALLOWLIST = Map.of("name", "p.name", "createdAt",
            "p.createdAt");
    private static final Map<String, String> MEMBER_SORT_ALLOWLIST = Map.of("name", "u.name", "joinedAt",
            "pu.createdAt");

    private final UserRepository userRepository;
    private final ProjectUserRepository projectUserRepository;
    private final ProjectRepository projectRepository;
    private final TeamUserRepository teamUserRepository;
    private final TaskRepository taskRepository;

    public ProjectService(TeamUserRepository teamUserRepository, ProjectRepository projectRepository,
            ProjectUserRepository projectUserRepository, UserRepository userRepository, TaskRepository taskRepository) {
        this.teamUserRepository = teamUserRepository;
        this.projectRepository = projectRepository;
        this.projectUserRepository = projectUserRepository;
        this.userRepository = userRepository;
        this.taskRepository = taskRepository;
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
        Pageable safePageable = QueryUtils.toSafePageable(pageable, PROJECT_SORT_ALLOWLIST, "p.name",
                Sort.Direction.ASC, "p.id");
        return projectUserRepository.findProjects(userId, pattern, safePageable);
    }

    @Transactional
    public void deleteProject(Long userId, Long projectId) {
        if (!projectUserRepository.existsById(new ProjectUserId(userId, projectId))) {
            throw new NotFoundException("プロジェクトが存在しません");
        }

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new NotFoundException("プロジェクトが存在しません"));

        if (!userId.equals(project.getCreatedBy())) {
            throw new ForbiddenException("プロジェクトを削除する権限がありません");
        }

        taskRepository.deleteByProjectId(projectId);
        projectUserRepository.deleteByIdProjectId(projectId);
        projectRepository.delete(project);
    }

    @Transactional(readOnly = true)
    public Page<ProjectMemberResponse> findMembers(Long userId, Long projectId, Pageable pageable) {
        if (!projectUserRepository.existsById(new ProjectUserId(userId, projectId))) {
            throw new NotFoundException("プロジェクトが存在しません");
        }

        Pageable safePageable = QueryUtils.toSafePageable(pageable, MEMBER_SORT_ALLOWLIST, "pu.createdAt",
                Sort.Direction.ASC, "pu.id.userId");
        return projectUserRepository.findMembers(projectId, safePageable);
    }

    @Transactional
    public ProjectMemberResponse addMember(Long userId, Long projectId, AddProjectMemberRequest request) {
        Long targetUserId = request.userId();

        if (!projectUserRepository.existsById(new ProjectUserId(userId, projectId))) {
            throw new NotFoundException("プロジェクトが存在しません");
        }

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new NotFoundException("プロジェクトが存在しません"));

        if (!userId.equals(project.getCreatedBy())) {
            throw new ForbiddenException("プロジェクトメンバーを追加する権限がありません");
        }

        if (!teamUserRepository.existsById(new TeamUserId(project.getTeamId(), targetUserId))) {
            throw new NotFoundException("ユーザーが存在しません");
        }

        if (projectUserRepository.existsById(new ProjectUserId(targetUserId, projectId))) {
            throw new ConflictException("このユーザーは既に参加しています");
        }

        ProjectUser projectUser = projectUserRepository
                .save(new ProjectUser(targetUserId, projectId, project.getTeamId()));
        User user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new NotFoundException("ユーザーが存在しません"));

        return new ProjectMemberResponse(targetUserId, user.getName(), projectUser.getCreatedAt());
    }

    @Transactional
    public void removeMember(Long userId, Long projectId, Long targetUserId) {
        if (!projectUserRepository.existsById(new ProjectUserId(userId, projectId))) {
            throw new NotFoundException("プロジェクトが存在しません");
        }

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new NotFoundException("プロジェクトが存在しません"));

        if (!userId.equals(project.getCreatedBy())) {
            throw new ForbiddenException("プロジェクトメンバーを削除する権限がありません");
        }

        if (targetUserId.equals(project.getCreatedBy())) {
            throw new UnprocessableContentException("作成者はプロジェクトから削除できません");
        }

        if (!projectUserRepository.existsById(new ProjectUserId(targetUserId, projectId))) {
            throw new NotFoundException("メンバーが存在しません");
        }

        // TODO: 担当タスクの assigned_to = NULL（tasks ができてから。）

        projectUserRepository.deleteById(new ProjectUserId(targetUserId, projectId));
    }

}

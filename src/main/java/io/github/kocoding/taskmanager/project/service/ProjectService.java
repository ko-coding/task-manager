package io.github.kocoding.taskmanager.project.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.kocoding.taskmanager.common.exception.NotFoundException;
import io.github.kocoding.taskmanager.project.dto.CreateProjectRequest;
import io.github.kocoding.taskmanager.project.dto.ProjectResponse;
import io.github.kocoding.taskmanager.project.entity.Project;
import io.github.kocoding.taskmanager.project.entity.ProjectUser;
import io.github.kocoding.taskmanager.project.repository.ProjectRepository;
import io.github.kocoding.taskmanager.project.repository.ProjectUserRepository;
import io.github.kocoding.taskmanager.team.entity.TeamUserId;
import io.github.kocoding.taskmanager.team.repository.TeamUserRepository;

@Service
public class ProjectService {

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

}

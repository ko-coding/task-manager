package io.github.kocoding.taskmanager.project.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import io.github.kocoding.taskmanager.common.exception.BadRequestException;
import io.github.kocoding.taskmanager.common.exception.ForbiddenException;
import io.github.kocoding.taskmanager.common.exception.NotFoundException;
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

@ExtendWith(MockitoExtension.class)
public class ProjectServiceTest {

    @Mock
    private ProjectUserRepository projectUserRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private TeamUserRepository teamUserRepository;

    @InjectMocks
    private ProjectService projectService;

    @Test
    void create_作成者がプロジェクトメンバーとして保存される() {
        Long userId = 1L;
        Long teamId = 10L;
        CreateProjectRequest request = new CreateProjectRequest("test", "test");
        Project project = new Project(teamId, userId, request.name(), request.description());
        ReflectionTestUtils.setField(project, "id", 20L);

        when(teamUserRepository.existsById(new TeamUserId(teamId, userId)))
                .thenReturn(true);
        when(projectRepository.save(any(Project.class)))
                .thenReturn(project);

        projectService.create(userId, teamId, request);

        ArgumentCaptor<ProjectUser> projectUserCaptor = ArgumentCaptor.forClass(ProjectUser.class);
        verify(projectUserRepository).save(projectUserCaptor.capture());
        ProjectUser saved = projectUserCaptor.getValue();

        assertEquals(new ProjectUserId(1L, 20L), saved.getId());
        assertEquals(10L, saved.getTeamId());
    }

    @Test
    void create_所属していないチームにプロジェクトを作成しようとするとNotFoundExceptionが発生する() {
        Long userId = 1L;
        Long teamId = 10L;
        CreateProjectRequest request = new CreateProjectRequest("test", "test");

        when(teamUserRepository.existsById(new TeamUserId(teamId, userId)))
                .thenReturn(false);

        NotFoundException e = assertThrows(NotFoundException.class,
                () -> projectService.create(userId, teamId, request));
        assertEquals("チームが存在しません", e.getMessage());
        verify(projectRepository, never()).save(any(Project.class));
        verify(projectUserRepository, never()).save(any(ProjectUser.class));
    }

    @Test
    void findProject_メンバーならプロジェクトが返る() {
        Long userId = 1L;
        Long teamId = 10L;
        Long projectId = 20L;

        when(projectUserRepository.existsById(new ProjectUserId(userId, projectId)))
                .thenReturn(true);
        when(projectRepository.findById(projectId))
                .thenReturn(Optional.of(new Project(teamId, 2L, "test", "test")));

        ProjectResponse response = projectService.findProject(userId, projectId);

        assertEquals(10L, response.teamId());
        assertEquals(2L, response.createdBy());
        assertEquals("test", response.name());
        assertEquals("test", response.description());
    }

    @Test
    void findProject_メンバーでなければNotFoundExceptionが発生する() {
        Long userId = 1L;
        Long projectId = 20L;

        when(projectUserRepository.existsById(new ProjectUserId(userId, projectId)))
                .thenReturn(false);

        NotFoundException e = assertThrows(NotFoundException.class,
                () -> projectService.findProject(userId, projectId));
        assertEquals("プロジェクトが存在しません", e.getMessage());
        verify(projectRepository, never()).findById(any());
    }

    @Test
    void updateProject_正常に更新される() {
        Long userId = 1L;
        Long teamId = 10L;
        Long projectId = 20L;
        UpdateProjectRequest request = new UpdateProjectRequest();
        request.setName(Optional.of("new name"));
        request.setDescription(Optional.of("new description"));

        when(projectUserRepository.existsById(new ProjectUserId(userId, projectId)))
                .thenReturn(true);
        when(projectRepository.findById(projectId))
                .thenReturn(Optional.of(new Project(teamId, userId, "test", "test")));

        ProjectResponse response = projectService.updateProject(userId, projectId, request);

        assertEquals("new name", response.name());
        assertEquals("new description", response.description());
    }

    @Test
    void updateProject_nameだけ指定した場合はnameのみ更新される() {
        Long userId = 1L;
        Long teamId = 10L;
        Long projectId = 20L;
        UpdateProjectRequest request = new UpdateProjectRequest();
        request.setName(Optional.of("new name"));

        when(projectUserRepository.existsById(new ProjectUserId(userId, projectId)))
                .thenReturn(true);
        when(projectRepository.findById(projectId))
                .thenReturn(Optional.of(new Project(teamId, userId, "test", "test")));

        ProjectResponse response = projectService.updateProject(userId, projectId, request);

        assertEquals("new name", response.name());
        assertEquals("test", response.description());
    }

    @Test
    void updateProject_descriptionにnullを指定した場合nameは更新されずdescriptionがnullに更新される() {
        Long userId = 1L;
        Long teamId = 10L;
        Long projectId = 20L;
        UpdateProjectRequest request = new UpdateProjectRequest();
        request.setDescription(Optional.empty());

        when(projectUserRepository.existsById(new ProjectUserId(userId, projectId)))
                .thenReturn(true);
        when(projectRepository.findById(projectId))
                .thenReturn(Optional.of(new Project(teamId, userId, "test", "test")));

        ProjectResponse response = projectService.updateProject(userId, projectId, request);

        assertNull(response.description());
        assertEquals("test", response.name());
    }

    @Test
    void updateProject_メンバー以外が更新しようとするとNotFoundExceptionが発生する() {
        Long userId = 1L;
        Long projectId = 20L;
        UpdateProjectRequest request = new UpdateProjectRequest();
        request.setName(Optional.of("new name"));
        request.setDescription(Optional.of("new description"));

        when(projectUserRepository.existsById(new ProjectUserId(userId, projectId)))
                .thenReturn(false);

        NotFoundException e = assertThrows(NotFoundException.class,
                () -> projectService.updateProject(userId, projectId, request));
        assertEquals("プロジェクトが存在しません", e.getMessage());
        verify(projectRepository, never()).findById(any());
    }

    @Test
    void updateProject_作成者以外が更新しようとするとForbiddenExceptionが発生する() {
        Long userId = 1L;
        Long teamId = 10L;
        Long projectId = 20L;
        UpdateProjectRequest request = new UpdateProjectRequest();
        request.setName(Optional.of("new name"));
        request.setDescription(Optional.of("new description"));

        when(projectUserRepository.existsById(new ProjectUserId(userId, projectId)))
                .thenReturn(true);
        when(projectRepository.findById(projectId))
                .thenReturn(Optional.of(new Project(teamId, 30L, "test", "test")));

        ForbiddenException e = assertThrows(ForbiddenException.class,
                () -> projectService.updateProject(userId, projectId, request));
        assertEquals("プロジェクトを更新する権限がありません", e.getMessage());
    }

    @Test
    void updateProject_nameにnullを指定した場合はBadRequestExceptionが発生する() {
        Long userId = 1L;
        Long teamId = 10L;
        Long projectId = 20L;
        UpdateProjectRequest request = new UpdateProjectRequest();
        request.setName(Optional.empty());
        request.setDescription(Optional.of("new description"));

        when(projectUserRepository.existsById(new ProjectUserId(userId, projectId)))
                .thenReturn(true);
        when(projectRepository.findById(projectId))
                .thenReturn(Optional.of(new Project(teamId, userId, "test", "test")));

        BadRequestException e = assertThrows(BadRequestException.class,
                () -> projectService.updateProject(userId, projectId, request));
        assertEquals("プロジェクト名にnullは指定できません", e.getMessage());
    }

}

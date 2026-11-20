package io.github.kocoding.taskmanager.project.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import io.github.kocoding.taskmanager.common.exception.NotFoundException;
import io.github.kocoding.taskmanager.project.dto.CreateProjectRequest;
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

}

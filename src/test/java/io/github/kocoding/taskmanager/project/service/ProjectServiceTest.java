package io.github.kocoding.taskmanager.project.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

import io.github.kocoding.taskmanager.common.exception.BadRequestException;
import io.github.kocoding.taskmanager.common.exception.ConflictException;
import io.github.kocoding.taskmanager.common.exception.ForbiddenException;
import io.github.kocoding.taskmanager.common.exception.NotFoundException;
import io.github.kocoding.taskmanager.common.exception.UnprocessableContentException;
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
import io.github.kocoding.taskmanager.team.entity.TeamUserId;
import io.github.kocoding.taskmanager.team.repository.TeamUserRepository;
import io.github.kocoding.taskmanager.user.entity.User;
import io.github.kocoding.taskmanager.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
public class ProjectServiceTest {

    @Mock
    private ProjectUserRepository projectUserRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private TeamUserRepository teamUserRepository;

    @Mock
    private UserRepository userRepository;

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

    @Test
    void findMyProjects_keywordの特殊文字はエスケープされて部分一致のパターンになる() {
        projectService.findMyProjects(1L, "a_b%c\\d", PageRequest.of(0, 20));

        ArgumentCaptor<String> patternCaptor = ArgumentCaptor.forClass(String.class);
        verify(projectUserRepository).findProjects(eq(1L), patternCaptor.capture(), any(Pageable.class));

        assertEquals("%a\\_b\\%c\\\\d%", patternCaptor.getValue());
    }

    @Test
    void findMyProjects_許可されていないsortならBadRequestExceptionになりRepositoryは呼ばれない() {
        Pageable pageable = PageRequest.of(0, 20, Sort.by("foo"));

        assertThrows(BadRequestException.class, () -> projectService.findMyProjects(1L, null, pageable));

        verify(projectUserRepository, never()).findProjects(any(), any(), any());
    }

    @Test
    void findMyProjects_許可されたsortは置き換えられp_idが最後に足される() {
        Pageable pageable = PageRequest.of(2, 10, Sort.by(Sort.Direction.DESC, "createdAt"));

        projectService.findMyProjects(1L, null, pageable);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(projectUserRepository).findProjects(eq(1L), any(), pageableCaptor.capture());
        Pageable passed = pageableCaptor.getValue();

        List<Sort.Order> orders = passed.getSort().toList();
        assertEquals(2, orders.size());
        assertEquals(2, passed.getPageNumber());
        assertEquals(10, passed.getPageSize());

        assertEquals("p.createdAt", orders.get(0).getProperty());
        assertEquals(Sort.Direction.DESC, orders.get(0).getDirection());

        assertEquals("p.id", orders.get(1).getProperty());
        assertEquals(Sort.Direction.ASC, orders.get(1).getDirection());
    }

    @Test
    void findMyProjects_sortの指定がなければnameの昇順になりp_idが最後に足される() {
        Pageable pageable = PageRequest.of(2, 10);

        projectService.findMyProjects(1L, null, pageable);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(projectUserRepository).findProjects(eq(1L), any(), pageableCaptor.capture());
        Pageable passed = pageableCaptor.getValue();
        List<Sort.Order> orders = passed.getSort().toList();

        assertEquals(2, orders.size());
        assertEquals(2, passed.getPageNumber());
        assertEquals(10, passed.getPageSize());

        assertEquals("p.name", orders.get(0).getProperty());
        assertEquals(Sort.Direction.ASC, orders.get(0).getDirection());

        assertEquals("p.id", orders.get(1).getProperty());
        assertEquals(Sort.Direction.ASC, orders.get(1).getDirection());
    }

    @Test
    void deleteProject_作成者なら削除できる() {
        Long userId = 1L;
        Long teamId = 10L;
        Long projectId = 20L;
        Project project = new Project(teamId, userId, "test", "test");

        when(projectUserRepository.existsById(new ProjectUserId(userId, projectId)))
                .thenReturn(true);
        when(projectRepository.findById(projectId))
                .thenReturn(Optional.of(project));

        projectService.deleteProject(userId, projectId);

        InOrder inOrder = inOrder(projectUserRepository, projectRepository);
        inOrder.verify(projectUserRepository).deleteByIdProjectId(projectId);
        inOrder.verify(projectRepository).delete(project);
    }

    @Test
    void deleteProject_メンバーでなければNotFoundExceptionが発生する() {
        Long userId = 1L;
        Long projectId = 20L;

        when(projectUserRepository.existsById(new ProjectUserId(userId, projectId)))
                .thenReturn(false);

        NotFoundException e = assertThrows(NotFoundException.class,
                () -> projectService.deleteProject(userId, projectId));
        assertEquals("プロジェクトが存在しません", e.getMessage());
        verify(projectRepository, never()).findById(projectId);
        verify(projectUserRepository, never()).deleteByIdProjectId(projectId);
        verify(projectRepository, never()).delete(any());
    }

    @Test
    void deleteProject_作成者以外ならForbiddenExceptionが発生する() {
        Long userId = 1L;
        Long teamId = 10L;
        Long projectId = 20L;
        Project project = new Project(teamId, 30L, "test", "test");

        when(projectUserRepository.existsById(new ProjectUserId(userId, projectId)))
                .thenReturn(true);
        when(projectRepository.findById(projectId))
                .thenReturn(Optional.of(project));

        ForbiddenException e = assertThrows(ForbiddenException.class,
                () -> projectService.deleteProject(userId, projectId));
        assertEquals("プロジェクトを削除する権限がありません", e.getMessage());
        verify(projectUserRepository, never()).deleteByIdProjectId(projectId);
        verify(projectRepository, never()).delete(project);
    }

    @Test
    void findMembers_メンバーでなければNotFoundExceptionが発生する() {
        Long userId = 1L;
        Long projectId = 20L;
        Pageable pageable = PageRequest.of(0, 20);

        when(projectUserRepository.existsById(new ProjectUserId(userId, projectId)))
                .thenReturn(false);

        NotFoundException e = assertThrows(NotFoundException.class,
                () -> projectService.findMembers(userId, projectId, pageable));
        assertEquals("プロジェクトが存在しません", e.getMessage());
        verify(projectUserRepository, never()).findMembers(any(), any());
    }

    @Test
    void findMembers_許可されていないsortならBadRequestExceptionになりRepositoryは呼ばれない() {
        Pageable pageable = PageRequest.of(0, 20, Sort.by("foo"));

        when(projectUserRepository.existsById(new ProjectUserId(1L, 20L)))
                .thenReturn(true);

        assertThrows(BadRequestException.class, () -> projectService.findMembers(1L, 20L, pageable));
        verify(projectUserRepository, never()).findMembers(any(), any());
    }

    @Test
    void findMembers_許可されたsortは置き換えられpu_id_userIdが最後に足される() {
        Pageable pageable = PageRequest.of(2, 10, Sort.by(Sort.Direction.DESC, "joinedAt"));

        when(projectUserRepository.existsById(new ProjectUserId(1L, 20L)))
                .thenReturn(true);

        projectService.findMembers(1L, 20L, pageable);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(projectUserRepository).findMembers(eq(20L), pageableCaptor.capture());
        Pageable passed = pageableCaptor.getValue();

        List<Sort.Order> orders = passed.getSort().toList();
        assertEquals(2, orders.size());
        assertEquals(2, passed.getPageNumber());
        assertEquals(10, passed.getPageSize());

        assertEquals("pu.createdAt", orders.get(0).getProperty());
        assertEquals(Sort.Direction.DESC, orders.get(0).getDirection());

        assertEquals("pu.id.userId", orders.get(1).getProperty());
        assertEquals(Sort.Direction.ASC, orders.get(1).getDirection());
    }

    @Test
    void addMember_作成者が追加すると保存され追加したメンバーが返る() {
        Long userId = 1L;
        Long teamId = 10L;
        Long projectId = 20L;
        AddProjectMemberRequest request = new AddProjectMemberRequest(3L);
        Project project = new Project(teamId, userId, "test", "test");
        ProjectUser projectUser = new ProjectUser(request.userId(), projectId, teamId);
        ReflectionTestUtils.setField(projectUser, "createdAt", Instant.parse("2026-01-01T00:00:00Z"));

        when(projectUserRepository.existsById(new ProjectUserId(userId, projectId)))
                .thenReturn(true);
        when(projectRepository.findById(projectId))
                .thenReturn(Optional.of(project));
        when(teamUserRepository.existsById(new TeamUserId(project.getTeamId(), request.userId())))
                .thenReturn(true);
        when(projectUserRepository.existsById(new ProjectUserId(request.userId(), projectId)))
                .thenReturn(false);
        when(projectUserRepository.save(any(ProjectUser.class)))
                .thenReturn(projectUser);
        when(userRepository.findById(request.userId()))
                .thenReturn(Optional.of(new User("testuser", "test@example.com", "hashed")));

        ProjectMemberResponse response = projectService.addMember(userId, projectId, request);

        ArgumentCaptor<ProjectUser> projectUserCaptor = ArgumentCaptor.forClass(ProjectUser.class);
        verify(projectUserRepository).save(projectUserCaptor.capture());
        ProjectUser saved = projectUserCaptor.getValue();

        assertEquals(new ProjectUserId(3L, 20L), saved.getId());
        assertEquals(10L, saved.getTeamId());
        assertEquals(3L, response.userId());
        assertEquals("testuser", response.name());
        assertEquals(Instant.parse("2026-01-01T00:00:00Z"), response.joinedAt());
    }

    @Test
    void addMember_メンバーでなければNotFoundExceptionが発生する() {
        Long userId = 1L;
        Long projectId = 20L;
        AddProjectMemberRequest request = new AddProjectMemberRequest(3L);

        when(projectUserRepository.existsById(new ProjectUserId(userId, projectId)))
                .thenReturn(false);

        NotFoundException e = assertThrows(NotFoundException.class,
                () -> projectService.addMember(userId, projectId, request));
        assertEquals("プロジェクトが存在しません", e.getMessage());
        verify(projectRepository, never()).findById(any());
        verify(teamUserRepository, never()).existsById(any(TeamUserId.class));
        verify(projectUserRepository).existsById(any(ProjectUserId.class));
        verify(projectUserRepository, never()).save(any(ProjectUser.class));
        verify(userRepository, never()).findById(any(Long.class));
    }

    @Test
    void addMember_作成者以外が追加しようとするとForbiddenExceptionが発生する() {
        Long userId = 1L;
        Long teamId = 10L;
        Long projectId = 20L;
        AddProjectMemberRequest request = new AddProjectMemberRequest(3L);

        when(projectUserRepository.existsById(new ProjectUserId(userId, projectId)))
                .thenReturn(true);
        when(projectRepository.findById(projectId))
                .thenReturn(Optional.of(new Project(teamId, 100L, "test", "test")));

        ForbiddenException e = assertThrows(ForbiddenException.class,
                () -> projectService.addMember(userId, projectId, request));
        assertEquals("プロジェクトメンバーを追加する権限がありません", e.getMessage());
        verify(teamUserRepository, never()).existsById(any(TeamUserId.class));
        verify(projectUserRepository).existsById(any(ProjectUserId.class));
        verify(projectUserRepository, never()).save(any(ProjectUser.class));
        verify(userRepository, never()).findById(any(Long.class));
    }

    @Test
    void addMember_追加対象がチームにいなければNotFoundExceptionが発生する() {
        Long userId = 1L;
        Long teamId = 10L;
        Long projectId = 20L;
        AddProjectMemberRequest request = new AddProjectMemberRequest(3L);

        when(projectUserRepository.existsById(new ProjectUserId(userId, projectId)))
                .thenReturn(true);
        when(projectRepository.findById(projectId))
                .thenReturn(Optional.of(new Project(teamId, userId, "test", "test")));
        when(teamUserRepository.existsById(new TeamUserId(teamId, request.userId())))
                .thenReturn(false);

        NotFoundException e = assertThrows(NotFoundException.class,
                () -> projectService.addMember(userId, projectId, request));
        assertEquals("ユーザーが存在しません", e.getMessage());
        verify(projectUserRepository).existsById(any(ProjectUserId.class));
        verify(projectUserRepository, never()).save(any(ProjectUser.class));
        verify(userRepository, never()).findById(any(Long.class));
    }

    @Test
    void addMember_追加対象が既にメンバーならConflictExceptionが発生する() {
        Long userId = 1L;
        Long teamId = 10L;
        Long projectId = 20L;
        AddProjectMemberRequest request = new AddProjectMemberRequest(3L);

        when(projectUserRepository.existsById(new ProjectUserId(userId, projectId)))
                .thenReturn(true);
        when(projectRepository.findById(projectId))
                .thenReturn(Optional.of(new Project(teamId, userId, "test", "test")));
        when(teamUserRepository.existsById(new TeamUserId(teamId, request.userId())))
                .thenReturn(true);
        when(projectUserRepository.existsById(new ProjectUserId(request.userId(), projectId)))
                .thenReturn(true);

        ConflictException e = assertThrows(ConflictException.class,
                () -> projectService.addMember(userId, projectId, request));
        assertEquals("このユーザーは既に参加しています", e.getMessage());
        verify(projectUserRepository, never()).save(any(ProjectUser.class));
        verify(userRepository, never()).findById(any(Long.class));
    }

    @Test
    void removeMember_作成者ならメンバーを削除できる() {
        Long userId = 1L;
        Long targetUserId = 3L;
        Long teamId = 10L;
        Long projectId = 20L;

        when(projectUserRepository.existsById(new ProjectUserId(userId, projectId)))
                .thenReturn(true);
        when(projectRepository.findById(projectId))
                .thenReturn(Optional.of(new Project(teamId, userId, "test", "test")));
        when(projectUserRepository.existsById(new ProjectUserId(targetUserId, projectId)))
                .thenReturn(true);

        projectService.removeMember(userId, projectId, targetUserId);

        verify(projectUserRepository).deleteById(new ProjectUserId(3L, 20L));
    }

    @Test
    void removeMember_メンバーでなければNotFoundExceptionが発生する() {
        Long userId = 1L;
        Long targetUserId = 3L;
        Long projectId = 20L;

        when(projectUserRepository.existsById(new ProjectUserId(userId, projectId)))
                .thenReturn(false);

        NotFoundException e = assertThrows(NotFoundException.class,
                () -> projectService.removeMember(userId, projectId, targetUserId));
        assertEquals("プロジェクトが存在しません", e.getMessage());
        verify(projectRepository, never()).findById(projectId);
        verify(projectUserRepository, never()).existsById(new ProjectUserId(targetUserId, projectId));
        verify(projectUserRepository, never()).deleteById(any(ProjectUserId.class));
    }

    @Test
    void removeMember_作成者でなければForbiddenExceptionが発生する() {
        Long userId = 1L;
        Long targetUserId = 3L;
        Long teamId = 10L;
        Long projectId = 20L;

        when(projectUserRepository.existsById(new ProjectUserId(userId, projectId)))
                .thenReturn(true);
        when(projectRepository.findById(projectId))
                .thenReturn(Optional.of(new Project(teamId, 100L, "test", "test")));

        ForbiddenException e = assertThrows(ForbiddenException.class,
                () -> projectService.removeMember(userId, projectId, targetUserId));
        assertEquals("プロジェクトメンバーを削除する権限がありません", e.getMessage());
        verify(projectUserRepository, never()).existsById(new ProjectUserId(targetUserId, projectId));
        verify(projectUserRepository, never()).deleteById(any(ProjectUserId.class));
    }

    @Test
    void removeMember_作成者自身を削除しようとするとUnprocessableContentExceptionが発生する() {
        Long userId = 1L;
        Long targetUserId = 1L;
        Long teamId = 10L;
        Long projectId = 20L;

        when(projectUserRepository.existsById(new ProjectUserId(userId, projectId)))
                .thenReturn(true);
        when(projectRepository.findById(projectId))
                .thenReturn(Optional.of(new Project(teamId, userId, "test", "test")));

        UnprocessableContentException e = assertThrows(UnprocessableContentException.class,
                () -> projectService.removeMember(userId, projectId, targetUserId));
        assertEquals("作成者はプロジェクトから削除できません", e.getMessage());
        verify(projectUserRepository).existsById(any(ProjectUserId.class));
        verify(projectUserRepository, never()).deleteById(any(ProjectUserId.class));
    }

    @Test
    void removeMember_対象がメンバーでない場合はNotFoundExceptionが発生する() {
        Long userId = 1L;
        Long targetUserId = 3L;
        Long teamId = 10L;
        Long projectId = 20L;

        when(projectUserRepository.existsById(new ProjectUserId(userId, projectId)))
                .thenReturn(true);
        when(projectRepository.findById(projectId))
                .thenReturn(Optional.of(new Project(teamId, userId, "test", "test")));
        when(projectUserRepository.existsById(new ProjectUserId(targetUserId, projectId)))
                .thenReturn(false);

        NotFoundException e = assertThrows(NotFoundException.class,
                () -> projectService.removeMember(userId, projectId, targetUserId));
        assertEquals("メンバーが存在しません", e.getMessage());
        verify(projectUserRepository, never()).deleteById(any(ProjectUserId.class));
    }

}

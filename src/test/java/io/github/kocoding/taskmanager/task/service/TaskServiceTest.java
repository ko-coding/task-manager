package io.github.kocoding.taskmanager.task.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

import io.github.kocoding.taskmanager.common.exception.BadRequestException;
import io.github.kocoding.taskmanager.common.exception.ForbiddenException;
import io.github.kocoding.taskmanager.common.exception.NotFoundException;
import io.github.kocoding.taskmanager.common.exception.UnprocessableContentException;
import io.github.kocoding.taskmanager.project.entity.ProjectUserId;
import io.github.kocoding.taskmanager.project.repository.ProjectUserRepository;
import io.github.kocoding.taskmanager.task.dto.CreateTaskRequest;
import io.github.kocoding.taskmanager.task.dto.TaskResponse;
import io.github.kocoding.taskmanager.task.dto.UpdateTaskRequest;
import io.github.kocoding.taskmanager.task.entity.Task;
import io.github.kocoding.taskmanager.task.entity.TaskStatus;
import io.github.kocoding.taskmanager.task.repository.TaskRepository;

@ExtendWith(MockitoExtension.class)
public class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private ProjectUserRepository projectUserRepository;

    @InjectMocks
    private TaskService taskService;

    @Test
    void create_担当者なしで作成できる() {
        Long userId = 1L;
        Long projectId = 20L;
        CreateTaskRequest request = new CreateTaskRequest("title", "description", null, LocalDate.of(2026, 1, 1));

        when(projectUserRepository.existsById(new ProjectUserId(userId, projectId)))
                .thenReturn(true);
        when(taskRepository.save(any(Task.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        taskService.create(userId, projectId, request);
        ArgumentCaptor<Task> taskCaptor = ArgumentCaptor.forClass(Task.class);
        verify(taskRepository).save(taskCaptor.capture());
        Task saved = taskCaptor.getValue();

        assertEquals(projectId, saved.getProjectId());
        assertEquals(userId, saved.getCreatedBy());
        assertEquals("title", saved.getTitle());
        assertEquals("description", saved.getDescription());
        assertEquals(LocalDate.of(2026, 1, 1), saved.getDueDate());
        assertEquals(TaskStatus.TODO, saved.getStatus());
        assertNull(saved.getAssignedTo());
        verify(projectUserRepository).existsById(any(ProjectUserId.class));
    }

    @Test
    void create_担当者ありで作成できる() {
        Long userId = 1L;
        Long projectId = 20L;
        CreateTaskRequest request = new CreateTaskRequest("title", "description", 5L, LocalDate.of(2026, 1, 1));

        when(projectUserRepository.existsById(new ProjectUserId(userId, projectId)))
                .thenReturn(true);
        when(projectUserRepository.existsById(new ProjectUserId(5L, projectId)))
                .thenReturn(true);
        when(taskRepository.save(any(Task.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        TaskResponse response = taskService.create(userId, projectId, request);
        ArgumentCaptor<Task> taskCaptor = ArgumentCaptor.forClass(Task.class);
        verify(taskRepository).save(taskCaptor.capture());
        Task saved = taskCaptor.getValue();

        assertEquals(projectId, saved.getProjectId());
        assertEquals(userId, saved.getCreatedBy());
        assertEquals("title", saved.getTitle());
        assertEquals(response.description(), saved.getDescription());
        assertEquals(LocalDate.of(2026, 1, 1), saved.getDueDate());
        assertEquals(TaskStatus.TODO, saved.getStatus());
        assertEquals(5L, saved.getAssignedTo());
    }

    @Test
    void create_本人がメンバーでない場合はNotFoundExceptionが発生する() {
        Long userId = 1L;
        Long projectId = 20L;
        CreateTaskRequest request = new CreateTaskRequest("title", "description", 5L, LocalDate.of(2026, 1, 1));

        when(projectUserRepository.existsById(new ProjectUserId(userId, projectId)))
                .thenReturn(false);

        NotFoundException e = assertThrows(NotFoundException.class,
                () -> taskService.create(userId, projectId, request));
        assertEquals("プロジェクトが存在しません", e.getMessage());
        verify(projectUserRepository, never()).existsById(new ProjectUserId(5L, projectId));
        verify(taskRepository, never()).save(any(Task.class));
    }

    @Test
    void create_メンバー以外を担当者に指定した場合はUnprocessableContentExceptionが発生する() {
        Long userId = 1L;
        Long projectId = 20L;
        CreateTaskRequest request = new CreateTaskRequest("title", "description", 100L, LocalDate.of(2026, 1, 1));

        when(projectUserRepository.existsById(new ProjectUserId(userId, projectId)))
                .thenReturn(true);
        when(projectUserRepository.existsById(new ProjectUserId(request.assignedTo(), projectId)))
                .thenReturn(false);

        UnprocessableContentException e = assertThrows(UnprocessableContentException.class,
                () -> taskService.create(userId, projectId, request));
        assertEquals("担当者はプロジェクトのメンバーである必要があります", e.getMessage());
        verify(taskRepository, never()).save(any(Task.class));
    }

    @Test
    void findTask_メンバーでタスクが存在する場合はそのタスクが返る() {
        Long userId = 1L;
        Long projectId = 20L;
        Long taskId = 50L;
        Task task = new Task(projectId, 100L, 2L, "title", "description", TaskStatus.TODO,
                LocalDate.of(2026, 1, 1));
        ReflectionTestUtils.setField(task, "id", 50L);

        when(taskRepository.findById(taskId))
                .thenReturn(Optional.of(task));
        when(projectUserRepository.existsById(new ProjectUserId(userId, projectId)))
                .thenReturn(true);

        TaskResponse response = taskService.findTask(userId, taskId);

        assertEquals(50L, response.id());
        assertEquals("title", response.title());
    }

    @Test
    void findTask_タスクが存在しない場合はNotFoundExceptionが発生する() {
        when(taskRepository.findById(50L))
                .thenReturn(Optional.empty());

        NotFoundException e = assertThrows(NotFoundException.class, () -> taskService.findTask(1L, 50L));
        assertEquals("タスクが存在しません", e.getMessage());
        verify(projectUserRepository, never()).existsById(any(ProjectUserId.class));
    }

    @Test
    void findTask_メンバーでない場合はNotFoundExceptionが発生する() {
        Long userId = 1L;
        Long taskId = 50L;
        Task task = new Task(500L, 100L, 2L, "title", "description", TaskStatus.TODO,
                LocalDate.of(2026, 1, 1));

        when(taskRepository.findById(taskId))
                .thenReturn(Optional.of(task));
        when(projectUserRepository.existsById(new ProjectUserId(userId, task.getProjectId())))
                .thenReturn(false);

        NotFoundException e = assertThrows(NotFoundException.class, () -> taskService.findTask(userId, taskId));
        assertEquals("タスクが存在しません", e.getMessage());
    }

    @Test
    void findTasks_本人がメンバーでない場合はNotFoundExceptionが発生する() {
        Long userId = 1L;
        Long projectId = 20L;

        when(projectUserRepository.existsById(new ProjectUserId(userId, projectId)))
                .thenReturn(false);

        NotFoundException e = assertThrows(NotFoundException.class,
                () -> taskService.findTasks(userId, projectId, null, null, null, null, null, null));
        assertEquals("プロジェクトが存在しません", e.getMessage());
        verify(taskRepository, never()).findTasks(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void findTasks_dueDateFromがdueDateToより後の場合はBadRequestExceptionが発生する() {
        Long userId = 1L;
        Long projectId = 20L;
        LocalDate dueDateFrom = LocalDate.of(2026, 1, 2);
        LocalDate dueDateTo = LocalDate.of(2026, 1, 1);

        when(projectUserRepository.existsById(new ProjectUserId(userId, projectId)))
                .thenReturn(true);

        BadRequestException e = assertThrows(BadRequestException.class,
                () -> taskService.findTasks(userId, projectId, null, null, null, dueDateFrom, dueDateTo, null));
        assertEquals("期間の指定が正しくありません", e.getMessage());
        verify(taskRepository, never()).findTasks(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void findTasks_許可されていないsortはBadRequestExceptionが発生する() {
        Long userId = 1L;
        Long projectId = 20L;
        Pageable pageable = PageRequest.of(0, 20, Sort.by("foo"));

        when(projectUserRepository.existsById(new ProjectUserId(userId, projectId)))
                .thenReturn(true);

        assertThrows(BadRequestException.class,
                () -> taskService.findTasks(userId, projectId, null, null, null, null, null, pageable));
        verify(taskRepository, never()).findTasks(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void findTasks_許可されたsortは置き換えられt_idが最後に足される() {
        Long userId = 1L;
        Long projectId = 20L;
        Pageable pageable = PageRequest.of(2, 10, Sort.by(Sort.Direction.DESC, "dueDate"));

        when(projectUserRepository.existsById(new ProjectUserId(userId, projectId)))
                .thenReturn(true);

        taskService.findTasks(userId, projectId, null, null, null, null, null, pageable);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(taskRepository).findTasks(eq(20L), any(), any(), any(), any(), any(), pageableCaptor.capture());
        Pageable passed = pageableCaptor.getValue();

        List<Sort.Order> orders = passed.getSort().toList();
        assertEquals(2, orders.size());
        assertEquals(2, passed.getPageNumber());
        assertEquals(10, passed.getPageSize());

        assertEquals("t.dueDate", orders.get(0).getProperty());
        assertEquals(Sort.Direction.DESC, orders.get(0).getDirection());
        assertEquals(Sort.NullHandling.NULLS_LAST, orders.get(0).getNullHandling());

        assertEquals("t.id", orders.get(1).getProperty());
        assertEquals(Sort.Direction.ASC, orders.get(1).getDirection());
        assertEquals(Sort.NullHandling.NULLS_LAST, orders.get(1).getNullHandling());
    }

    @Test
    void findTasks_sortの指定がなければcreatedAtの降順になりt_idが最後に足される() {
        Long userId = 1L;
        Long projectId = 20L;
        Pageable pageable = PageRequest.of(2, 10);

        when(projectUserRepository.existsById(new ProjectUserId(userId, projectId)))
                .thenReturn(true);

        taskService.findTasks(userId, projectId, null, null, null, null, null, pageable);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(taskRepository).findTasks(eq(20L), any(), any(), any(), any(), any(), pageableCaptor.capture());
        Pageable passed = pageableCaptor.getValue();
        List<Sort.Order> orders = passed.getSort().toList();

        assertEquals(2, orders.size());
        assertEquals(2, passed.getPageNumber());
        assertEquals(10, passed.getPageSize());

        assertEquals("t.createdAt", orders.get(0).getProperty());
        assertEquals(Sort.Direction.DESC, orders.get(0).getDirection());
        assertEquals(Sort.NullHandling.NULLS_LAST, orders.get(0).getNullHandling());

        assertEquals("t.id", orders.get(1).getProperty());
        assertEquals(Sort.Direction.ASC, orders.get(1).getDirection());
        assertEquals(Sort.NullHandling.NULLS_LAST, orders.get(1).getNullHandling());
    }

    @Test
    void findTasks_絞り込みの値が取り違えずに渡る() {
        Pageable pageable = PageRequest.of(2, 10);
        Page<TaskResponse> page = new PageImpl<>(List.of());

        when(projectUserRepository.existsById(new ProjectUserId(1L, 20L)))
                .thenReturn(true);
        when(taskRepository.findTasks(any(), any(), any(), any(), any(), any(), any())).thenReturn(page);

        Page<TaskResponse> response = taskService.findTasks(1L, 20L, TaskStatus.IN_PROGRESS, 100L, "abc",
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 1, 31), pageable);

        verify(taskRepository).findTasks(eq(20L), eq(TaskStatus.IN_PROGRESS), eq(100L), eq("%abc%"),
                eq(LocalDate.of(2026, 1, 1)), eq(LocalDate.of(2026, 1, 31)), any(Pageable.class));
        assertSame(page, response);
    }

    @Test
    void updateTask_作成者が指定した項目のみ更新される() {
        Long userId = 1L;
        Long taskId = 50L;
        UpdateTaskRequest request = new UpdateTaskRequest();
        request.setTitle(Optional.of("new title"));
        Task task = new Task(20L, 100L, userId, "title", "description", TaskStatus.IN_PROGRESS,
                LocalDate.of(2026, 1, 1));

        when(taskRepository.findById(taskId))
                .thenReturn(Optional.of(task));
        when(projectUserRepository.existsById(new ProjectUserId(userId, task.getProjectId())))
                .thenReturn(true);

        TaskResponse response = taskService.updateTask(userId, taskId, request);

        assertEquals("new title", response.title());
        assertEquals("description", response.description());
        assertEquals(LocalDate.of(2026, 1, 1), response.dueDate());
        assertEquals(100L, response.assignedTo());
        verify(projectUserRepository).existsById(any(ProjectUserId.class));
    }

    @Test
    void updateTask_同じプロジェクトメンバーを担当者として更新できる() {
        Long userId = 1L;
        Long taskId = 50L;
        UpdateTaskRequest request = new UpdateTaskRequest();
        request.setAssignedTo(Optional.of(200L));
        Task task = new Task(20L, 100L, userId, "title", "description", TaskStatus.IN_PROGRESS,
                LocalDate.of(2026, 1, 1));

        when(taskRepository.findById(taskId))
                .thenReturn(Optional.of(task));
        when(projectUserRepository.existsById(new ProjectUserId(userId, task.getProjectId())))
                .thenReturn(true);
        when(projectUserRepository.existsById(new ProjectUserId(request.getAssignedTo().get(), task.getProjectId())))
                .thenReturn(true);

        TaskResponse response = taskService.updateTask(userId, taskId, request);

        assertEquals(200L, response.assignedTo());
    }

    @Test
    void updateTask_タスクが存在しない場合はNotFoundExceptionが発生する() {
        Long userId = 1L;
        Long taskId = 50L;
        UpdateTaskRequest request = new UpdateTaskRequest();
        request.setTitle(Optional.of("new title"));

        when(taskRepository.findById(taskId))
                .thenReturn(Optional.empty());

        NotFoundException e = assertThrows(NotFoundException.class,
                () -> taskService.updateTask(userId, taskId, request));
        assertEquals("タスクが存在しません", e.getMessage());
        verify(projectUserRepository, never()).existsById(any(ProjectUserId.class));
    }

    @Test
    void updateTask_本人がメンバーでない場合はNotFoundExceptionが発生する() {
        Long userId = 1L;
        Long taskId = 50L;
        UpdateTaskRequest request = new UpdateTaskRequest();
        request.setTitle(Optional.of("new title"));
        Task task = new Task(20L, null, 100L, "title", "description", TaskStatus.IN_PROGRESS, null);

        when(taskRepository.findById(taskId))
                .thenReturn(Optional.of(task));
        when(projectUserRepository.existsById(new ProjectUserId(userId, task.getProjectId())))
                .thenReturn(false);

        NotFoundException e = assertThrows(NotFoundException.class,
                () -> taskService.updateTask(userId, taskId, request));
        assertEquals("タスクが存在しません", e.getMessage());
        verify(projectUserRepository).existsById(any(ProjectUserId.class));
    }

    @Test
    void updateTask_本人が作成者でない場合はForbiddenExceptionが発生する() {
        Long userId = 1L;
        Long taskId = 50L;
        UpdateTaskRequest request = new UpdateTaskRequest();
        request.setTitle(Optional.of("new title"));
        Task task = new Task(20L, null, 100L, "title", "description", TaskStatus.IN_PROGRESS, null);

        when(taskRepository.findById(taskId))
                .thenReturn(Optional.of(task));
        when(projectUserRepository.existsById(new ProjectUserId(userId, task.getProjectId())))
                .thenReturn(true);

        ForbiddenException e = assertThrows(ForbiddenException.class,
                () -> taskService.updateTask(userId, taskId, request));
        assertEquals("タスクを更新する権限がありません", e.getMessage());
        verify(projectUserRepository).existsById(any(ProjectUserId.class));
    }

    @Test
    void updateTask_タスク名にnullを指定した場合はBadRequestExceptionが発生する() {
        Long userId = 1L;
        Long taskId = 50L;
        UpdateTaskRequest request = new UpdateTaskRequest();
        request.setTitle(Optional.empty());
        Task task = new Task(20L, null, userId, "title", "description", TaskStatus.IN_PROGRESS, null);

        when(taskRepository.findById(taskId))
                .thenReturn(Optional.of(task));
        when(projectUserRepository.existsById(new ProjectUserId(userId, task.getProjectId())))
                .thenReturn(true);

        BadRequestException e = assertThrows(BadRequestException.class,
                () -> taskService.updateTask(userId, taskId, request));
        assertEquals("タスク名にnullは指定できません", e.getMessage());
        verify(projectUserRepository).existsById(any(ProjectUserId.class));
    }

    @Test
    void updateTask_担当者にプロジェクトメンバー以外を指定した場合はUnprocessableContentExceptionが発生する() {
        Long userId = 1L;
        Long taskId = 50L;
        UpdateTaskRequest request = new UpdateTaskRequest();
        request.setAssignedTo(Optional.of(100L));
        Task task = new Task(20L, null, userId, "title", "description", TaskStatus.IN_PROGRESS, null);

        when(taskRepository.findById(taskId))
                .thenReturn(Optional.of(task));
        when(projectUserRepository.existsById(new ProjectUserId(userId, task.getProjectId())))
                .thenReturn(true);
        when(projectUserRepository.existsById(new ProjectUserId(request.getAssignedTo().get(), task.getProjectId())))
                .thenReturn(false);

        UnprocessableContentException e = assertThrows(UnprocessableContentException.class,
                () -> taskService.updateTask(userId, taskId, request));
        assertEquals("担当者はプロジェクトのメンバーである必要があります", e.getMessage());
    }

    @Test
    void updateTask_タスク名以外はnullを指定して解除できる() {
        Long userId = 1L;
        Long taskId = 50L;
        UpdateTaskRequest request = new UpdateTaskRequest();
        request.setDescription(Optional.empty());
        request.setDueDate(Optional.empty());
        request.setAssignedTo(Optional.empty());
        Task task = new Task(20L, 100L, userId, "title", "description", TaskStatus.IN_PROGRESS,
                LocalDate.of(2026, 1, 1));

        when(taskRepository.findById(taskId))
                .thenReturn(Optional.of(task));
        when(projectUserRepository.existsById(new ProjectUserId(userId, task.getProjectId())))
                .thenReturn(true);

        TaskResponse response = taskService.updateTask(userId, taskId, request);

        assertEquals("title", response.title());
        assertNull(response.description());
        assertNull(response.dueDate());
        assertNull(response.assignedTo());
        verify(projectUserRepository).existsById(any(ProjectUserId.class));
    }

}

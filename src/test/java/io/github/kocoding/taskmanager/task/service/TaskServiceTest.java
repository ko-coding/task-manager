package io.github.kocoding.taskmanager.task.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import io.github.kocoding.taskmanager.common.exception.NotFoundException;
import io.github.kocoding.taskmanager.common.exception.UnprocessableContentException;
import io.github.kocoding.taskmanager.project.entity.ProjectUserId;
import io.github.kocoding.taskmanager.project.repository.ProjectUserRepository;
import io.github.kocoding.taskmanager.task.dto.CreateTaskRequest;
import io.github.kocoding.taskmanager.task.dto.TaskResponse;
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

}

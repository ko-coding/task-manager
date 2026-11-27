package io.github.kocoding.taskmanager.task.service;

import java.time.LocalDate;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.kocoding.taskmanager.common.exception.BadRequestException;
import io.github.kocoding.taskmanager.common.exception.ForbiddenException;
import io.github.kocoding.taskmanager.common.exception.NotFoundException;
import io.github.kocoding.taskmanager.common.exception.UnprocessableContentException;
import io.github.kocoding.taskmanager.common.query.QueryUtils;
import io.github.kocoding.taskmanager.project.entity.ProjectUserId;
import io.github.kocoding.taskmanager.project.repository.ProjectUserRepository;
import io.github.kocoding.taskmanager.task.dto.CreateTaskRequest;
import io.github.kocoding.taskmanager.task.dto.TaskResponse;
import io.github.kocoding.taskmanager.task.dto.UpdateTaskRequest;
import io.github.kocoding.taskmanager.task.entity.Task;
import io.github.kocoding.taskmanager.task.entity.TaskStatus;
import io.github.kocoding.taskmanager.task.repository.TaskRepository;

@Service
public class TaskService {

    private static final Map<String, String> TASK_SORT_ALLOWLIST = Map.of("dueDate", "t.dueDate", "createdAt",
            "t.createdAt");

    private final TaskRepository taskRepository;
    private final ProjectUserRepository projectUserRepository;

    public TaskService(ProjectUserRepository projectUserRepository, TaskRepository taskRepository) {
        this.projectUserRepository = projectUserRepository;
        this.taskRepository = taskRepository;
    }

    @Transactional
    public TaskResponse create(Long userId, Long projectId, CreateTaskRequest request) {
        if (!projectUserRepository.existsById(new ProjectUserId(userId, projectId))) {
            throw new NotFoundException("プロジェクトが存在しません");
        }

        if (request.assignedTo() != null
                && !projectUserRepository.existsById(new ProjectUserId(request.assignedTo(), projectId))) {
            throw new UnprocessableContentException("担当者はプロジェクトのメンバーである必要があります");
        }

        Task task = new Task(projectId, request.assignedTo(), userId, request.title(), request.description(),
                TaskStatus.TODO, request.dueDate());

        return TaskResponse.from(taskRepository.save(task));
    }

    @Transactional(readOnly = true)
    public TaskResponse findTask(Long userId, Long taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new NotFoundException("タスクが存在しません"));

        if (!projectUserRepository.existsById(new ProjectUserId(userId, task.getProjectId()))) {
            throw new NotFoundException("タスクが存在しません");
        }

        return TaskResponse.from(task);
    }

    @Transactional(readOnly = true)
    public Page<TaskResponse> findTasks(Long userId, Long projectId, TaskStatus status, Long assignedTo, String keyword,
            LocalDate dueDateFrom, LocalDate dueDateTo, Pageable pageable) {
        if (!projectUserRepository.existsById(new ProjectUserId(userId, projectId))) {
            throw new NotFoundException("プロジェクトが存在しません");
        }

        if (dueDateFrom != null && dueDateTo != null && dueDateFrom.isAfter(dueDateTo)) {
            throw new BadRequestException("期間の指定が正しくありません");
        }

        String pattern = QueryUtils.toLikePattern(keyword);
        Pageable safePageable = QueryUtils.toSafePageable(pageable, TASK_SORT_ALLOWLIST, "t.createdAt",
                Sort.Direction.DESC, "t.id");
        return taskRepository.findTasks(projectId, status, assignedTo, pattern, dueDateFrom, dueDateTo, safePageable);
    }

    @Transactional
    public TaskResponse updateTask(Long userId, Long taskId, UpdateTaskRequest request) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new NotFoundException("タスクが存在しません"));

        if (!projectUserRepository.existsById(new ProjectUserId(userId, task.getProjectId()))) {
            throw new NotFoundException("タスクが存在しません");
        }

        if (!userId.equals(task.getCreatedBy())) {
            throw new ForbiddenException("タスクを更新する権限がありません");
        }

        if (request.getTitle() != null && request.getTitle().isEmpty()) {
            throw new BadRequestException("タスク名にnullは指定できません");
        }

        if (request.getAssignedTo() != null && request.getAssignedTo().isPresent() && !projectUserRepository
                .existsById(new ProjectUserId(request.getAssignedTo().get(), task.getProjectId()))) {
            throw new UnprocessableContentException("担当者はプロジェクトのメンバーである必要があります");
        }

        if (request.getTitle() != null) {
            task.changeTitle(request.getTitle().get());
        }

        if (request.getDescription() != null) {
            task.changeDescription(request.getDescription().orElse(null));
        }

        if (request.getDueDate() != null) {
            task.changeDueDate(request.getDueDate().orElse(null));
        }

        if (request.getAssignedTo() != null) {
            task.changeAssignedTo(request.getAssignedTo().orElse(null));
        }

        taskRepository.flush();
        return TaskResponse.from(task);
    }

}

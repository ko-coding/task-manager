package io.github.kocoding.taskmanager.task.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.kocoding.taskmanager.common.exception.NotFoundException;
import io.github.kocoding.taskmanager.common.exception.UnprocessableContentException;
import io.github.kocoding.taskmanager.project.entity.ProjectUserId;
import io.github.kocoding.taskmanager.project.repository.ProjectUserRepository;
import io.github.kocoding.taskmanager.task.dto.CreateTaskRequest;
import io.github.kocoding.taskmanager.task.dto.TaskResponse;
import io.github.kocoding.taskmanager.task.entity.Task;
import io.github.kocoding.taskmanager.task.entity.TaskStatus;
import io.github.kocoding.taskmanager.task.repository.TaskRepository;

@Service
public class TaskService {

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

}

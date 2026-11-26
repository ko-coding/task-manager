package io.github.kocoding.taskmanager.task.repository;

import java.time.LocalDate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import io.github.kocoding.taskmanager.task.dto.TaskResponse;
import io.github.kocoding.taskmanager.task.entity.Task;
import io.github.kocoding.taskmanager.task.entity.TaskStatus;

public interface TaskRepository extends JpaRepository<Task, Long> {

    @Query(value = """
            select new io.github.kocoding.taskmanager.task.dto.TaskResponse(
                t.id, t.projectId, t.assignedTo, t.createdBy, t.title, t.description, t.status, t.dueDate, t.createdAt, t.updatedAt)
            from Task t
            where t.projectId = :projectId
                and (:status is null or t.status = :status)
                and (:assignedTo is null or t.assignedTo = :assignedTo)
                and lower(t.title) like lower(:pattern) escape '\\'
                and (:dueDateFrom is null or t.dueDate >= :dueDateFrom)
                and (:dueDateTo is null or t.dueDate <= :dueDateTo)
            """, countQuery = """
            select count(t)
            from Task t
            where t.projectId = :projectId
                and (:status is null or t.status = :status)
                and (:assignedTo is null or t.assignedTo = :assignedTo)
                and lower(t.title) like lower(:pattern) escape '\\'
                and (:dueDateFrom is null or t.dueDate >= :dueDateFrom)
                and (:dueDateTo is null or t.dueDate <= :dueDateTo)
            """)
    Page<TaskResponse> findTasks(Long projectId, TaskStatus status, Long assignedTo, String pattern,
            LocalDate dueDateFrom, LocalDate dueDateTo, Pageable pageable);

}

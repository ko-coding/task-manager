package io.github.kocoding.taskmanager.task.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import io.github.kocoding.taskmanager.task.entity.Task;

public interface TaskRepository extends JpaRepository<Task, Long> {

}

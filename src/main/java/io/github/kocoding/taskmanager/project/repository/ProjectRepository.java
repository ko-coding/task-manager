package io.github.kocoding.taskmanager.project.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import io.github.kocoding.taskmanager.project.entity.Project;

public interface ProjectRepository extends JpaRepository<Project, Long> {

}

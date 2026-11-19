package io.github.kocoding.taskmanager.project.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import io.github.kocoding.taskmanager.project.entity.ProjectUser;
import io.github.kocoding.taskmanager.project.entity.ProjectUserId;

public interface ProjectUserRepository extends JpaRepository<ProjectUser, ProjectUserId> {

}

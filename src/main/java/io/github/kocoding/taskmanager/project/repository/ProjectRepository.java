package io.github.kocoding.taskmanager.project.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import io.github.kocoding.taskmanager.project.entity.Project;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    @Modifying
    @Query("delete from Project p where p.teamId = :teamId")
    void deleteByTeamId(Long teamId);

}

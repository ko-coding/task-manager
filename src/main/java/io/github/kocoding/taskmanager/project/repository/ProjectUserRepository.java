package io.github.kocoding.taskmanager.project.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import io.github.kocoding.taskmanager.project.dto.ProjectResponse;
import io.github.kocoding.taskmanager.project.entity.ProjectUser;
import io.github.kocoding.taskmanager.project.entity.ProjectUserId;

public interface ProjectUserRepository extends JpaRepository<ProjectUser, ProjectUserId> {

    @Query(value = """
            select new io.github.kocoding.taskmanager.project.dto.ProjectResponse(
                p.id, p.teamId, p.name, p.description, p.createdBy, p.createdAt)
            from Project p join ProjectUser pu on p.id = pu.id.projectId
            where pu.id.userId = :userId
                and lower(p.name) like lower(:pattern) escape '\\'
            """, countQuery = """
            select count(pu)
            from Project p join ProjectUser pu on p.id = pu.id.projectId
            where pu.id.userId = :userId
                and lower(p.name) like lower(:pattern) escape '\\'
            """)
    Page<ProjectResponse> findProjects(Long userId, String pattern, Pageable pageable);

}

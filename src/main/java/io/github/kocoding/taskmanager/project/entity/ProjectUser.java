package io.github.kocoding.taskmanager.project.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "project_users")
public class ProjectUser {

    @EmbeddedId
    private ProjectUserId id;

    @Column(name = "team_id", nullable = false)
    private Long teamId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ProjectUser() {
    }

    public ProjectUser(Long userId, Long projectId, Long teamId) {
        this.id = new ProjectUserId(userId, projectId);
        this.teamId = teamId;
    }

    @PrePersist
    public void prePersist() {
        this.createdAt = Instant.now();
    }

    public ProjectUserId getId() {
        return id;
    }

    public Long getTeamId() {
        return teamId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

}

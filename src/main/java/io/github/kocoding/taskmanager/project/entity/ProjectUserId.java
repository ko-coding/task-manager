package io.github.kocoding.taskmanager.project.entity;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public record ProjectUserId(
        @Column(name = "user_id") Long userId,
        @Column(name = "project_id") Long projectId) implements Serializable {
}

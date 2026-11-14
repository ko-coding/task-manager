package io.github.kocoding.taskmanager.team.entity;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public record TeamUserId(
        @Column(name = "team_id") Long teamId,
        @Column(name = "user_id") Long userId) implements Serializable {
}

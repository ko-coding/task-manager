package io.github.kocoding.taskmanager.team.entity;

import io.github.kocoding.taskmanager.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

@Entity
@Table(name = "team_users")
public class TeamUser extends BaseTimeEntity {

    @EmbeddedId
    private TeamUserId id;

    @Column(name = "role", nullable = false)
    @Enumerated(EnumType.STRING)
    private TeamRole role;

    protected TeamUser() {
    }

    public TeamUser(Long teamId, Long userId, TeamRole role) {
        this.id = new TeamUserId(teamId, userId);
        this.role = role;
    }

    public void changeRole(TeamRole role) {
        this.role = role;
    }

    public TeamUserId getId() {
        return id;
    }

    public TeamRole getRole() {
        return role;
    }

}

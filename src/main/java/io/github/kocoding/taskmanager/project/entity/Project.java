package io.github.kocoding.taskmanager.project.entity;

import io.github.kocoding.taskmanager.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "projects")
public class Project extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "team_id", nullable = false)
    private Long teamId;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    protected Project() {
    }

    public Project(Long teamId, Long createdBy, String name, String description) {
        this.teamId = teamId;
        this.createdBy = createdBy;
        this.name = name;
        this.description = description;
    }

    public void rename(String name) {
        this.name = name;
    }

    public void changeDescription(String description) {
        this.description = description;
    }

    public Long getId() {
        return id;
    }

    public Long getTeamId() {
        return teamId;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

}

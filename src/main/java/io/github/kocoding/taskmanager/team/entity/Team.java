package io.github.kocoding.taskmanager.team.entity;

import io.github.kocoding.taskmanager.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "teams")
public class Team extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(name = "code_hash", nullable = false, unique = true)
    private String codeHash;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    protected Team() {
    }

    public Team(Long createdBy, String codeHash, String name, String description) {
        this.createdBy = createdBy;
        this.codeHash = codeHash;
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

    public Long getCreatedBy() {
        return createdBy;
    }

    public String getCodeHash() {
        return codeHash;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

}

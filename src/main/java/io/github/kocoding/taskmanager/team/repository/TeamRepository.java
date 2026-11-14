package io.github.kocoding.taskmanager.team.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import io.github.kocoding.taskmanager.team.entity.Team;

public interface TeamRepository extends JpaRepository<Team, Long> {

}

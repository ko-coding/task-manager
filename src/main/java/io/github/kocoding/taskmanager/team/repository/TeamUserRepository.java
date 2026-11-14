package io.github.kocoding.taskmanager.team.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import io.github.kocoding.taskmanager.team.entity.TeamUser;
import io.github.kocoding.taskmanager.team.entity.TeamUserId;

public interface TeamUserRepository extends JpaRepository<TeamUser, TeamUserId> {

}

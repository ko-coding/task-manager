package io.github.kocoding.taskmanager.team.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import io.github.kocoding.taskmanager.team.dto.TeamResponse;
import io.github.kocoding.taskmanager.team.entity.TeamUser;
import io.github.kocoding.taskmanager.team.entity.TeamUserId;

public interface TeamUserRepository extends JpaRepository<TeamUser, TeamUserId> {

    @Query(value = """
            select new io.github.kocoding.taskmanager.team.dto.TeamResponse(
                t.id, t.name, t.description, tu.role, t.createdAt)
            from TeamUser tu join Team t on t.id = tu.id.teamId
            where tu.id.userId = :userId
                and lower(t.name) like lower(:pattern) escape '\\'
            """, countQuery = """
            select count(tu)
            from TeamUser tu join Team t on t.id = tu.id.teamId
            where tu.id.userId = :userId
                and lower(t.name) like lower(:pattern) escape '\\'
            """)
    Page<TeamResponse> findTeams(Long userId, String pattern, Pageable pageable);

}

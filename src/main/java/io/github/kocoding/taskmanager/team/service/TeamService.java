package io.github.kocoding.taskmanager.team.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.kocoding.taskmanager.team.dto.CreateTeamRequest;
import io.github.kocoding.taskmanager.team.dto.CreateTeamResponse;
import io.github.kocoding.taskmanager.team.dto.TeamResponse;
import io.github.kocoding.taskmanager.team.entity.Team;
import io.github.kocoding.taskmanager.team.entity.TeamRole;
import io.github.kocoding.taskmanager.team.entity.TeamUser;
import io.github.kocoding.taskmanager.team.repository.TeamRepository;
import io.github.kocoding.taskmanager.team.repository.TeamUserRepository;

@Service
public class TeamService {

    private final TeamRepository teamRepository;
    private final TeamUserRepository teamUserRepository;
    private final TeamCodeGenerator teamCodeGenerator;
    private final TeamCodeHasher teamCodeHasher;

    public TeamService(TeamRepository teamRepository, TeamUserRepository teamUserRepository,
            TeamCodeGenerator teamCodeGenerator, TeamCodeHasher teamCodeHasher) {
        this.teamRepository = teamRepository;
        this.teamUserRepository = teamUserRepository;
        this.teamCodeGenerator = teamCodeGenerator;
        this.teamCodeHasher = teamCodeHasher;
    }

    @Transactional
    public CreateTeamResponse create(Long userId, CreateTeamRequest request) {
        String teamCode = teamCodeGenerator.generate();
        String codeHash = teamCodeHasher.hash(teamCode);
        Team team = teamRepository.save(new Team(userId, codeHash, request.name(), request.description()));
        teamUserRepository.save(new TeamUser(team.getId(), userId, TeamRole.OWNER));
        return new CreateTeamResponse(TeamResponse.from(team, TeamRole.OWNER), teamCode);
    }

}

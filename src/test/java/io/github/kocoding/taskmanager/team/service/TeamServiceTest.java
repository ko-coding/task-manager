package io.github.kocoding.taskmanager.team.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import io.github.kocoding.taskmanager.team.dto.CreateTeamRequest;
import io.github.kocoding.taskmanager.team.dto.CreateTeamResponse;
import io.github.kocoding.taskmanager.team.entity.Team;
import io.github.kocoding.taskmanager.team.entity.TeamRole;
import io.github.kocoding.taskmanager.team.entity.TeamUser;
import io.github.kocoding.taskmanager.team.entity.TeamUserId;
import io.github.kocoding.taskmanager.team.repository.TeamRepository;
import io.github.kocoding.taskmanager.team.repository.TeamUserRepository;

@ExtendWith(MockitoExtension.class)
public class TeamServiceTest {

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private TeamUserRepository teamUserRepository;

    @Mock
    private TeamCodeGenerator teamCodeGenerator;

    @Mock
    private TeamCodeHasher teamCodeHasher;

    @InjectMocks
    private TeamService teamService;

    @Test
    void create_コードはハッシュ化して保存され平文のコードが返る() {
        String code = "ABCDEFGH2345";
        String codeHash = "hashed";
        Long userId = 1L;
        CreateTeamRequest request = new CreateTeamRequest("test_team", "test");

        Team team = new Team(userId, codeHash, request.name(), request.description());
        ReflectionTestUtils.setField(team, "id", 10L);

        when(teamCodeGenerator.generate()).thenReturn(code);
        when(teamCodeHasher.hash(code)).thenReturn(codeHash);
        when(teamRepository.save(any(Team.class))).thenReturn(team);

        CreateTeamResponse response = teamService.create(userId, request);

        ArgumentCaptor<Team> teamCaptor = ArgumentCaptor.forClass(Team.class);
        verify(teamRepository).save(teamCaptor.capture());
        Team saved = teamCaptor.getValue();

        assertEquals(code, response.code());
        assertEquals(codeHash, saved.getCodeHash());
        assertEquals(userId, saved.getCreatedBy());
    }

    @Test
    void create_作成者がOWNERとして保存される() {
        String code = "ABCDEFGH2345";
        String codeHash = "hashed";
        Long userId = 1L;
        CreateTeamRequest request = new CreateTeamRequest("test_team", "test");

        Team team = new Team(userId, codeHash, request.name(), request.description());
        ReflectionTestUtils.setField(team, "id", 10L);

        when(teamCodeGenerator.generate()).thenReturn(code);
        when(teamCodeHasher.hash(code)).thenReturn(codeHash);
        when(teamRepository.save(any(Team.class))).thenReturn(team);

        teamService.create(userId, request);

        ArgumentCaptor<TeamUser> teamUserCaptor = ArgumentCaptor.forClass(TeamUser.class);
        verify(teamUserRepository).save(teamUserCaptor.capture());
        TeamUser saved = teamUserCaptor.getValue();

        assertEquals(TeamRole.OWNER, saved.getRole());
        assertEquals(new TeamUserId(10L, userId), saved.getId());
    }

}

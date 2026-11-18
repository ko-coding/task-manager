package io.github.kocoding.taskmanager.team.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

import io.github.kocoding.taskmanager.common.exception.BadRequestException;
import io.github.kocoding.taskmanager.common.exception.ConflictException;
import io.github.kocoding.taskmanager.common.exception.NotFoundException;
import io.github.kocoding.taskmanager.team.dto.CreateTeamRequest;
import io.github.kocoding.taskmanager.team.dto.CreateTeamResponse;
import io.github.kocoding.taskmanager.team.dto.JoinTeamRequest;
import io.github.kocoding.taskmanager.team.dto.TeamMemberResponse;
import io.github.kocoding.taskmanager.team.dto.TeamResponse;
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

    @Test
    void findMyTeams_keywordの特殊文字はエスケープされて部分一致のパターンになる() {
        teamService.findMyTeams(1L, "a_b%c\\d", PageRequest.of(0, 20));

        ArgumentCaptor<String> patternCaptor = ArgumentCaptor.forClass(String.class);
        verify(teamUserRepository).findTeams(eq(1L), patternCaptor.capture(), any(Pageable.class));

        assertEquals("%a\\_b\\%c\\\\d%", patternCaptor.getValue());
    }

    @Test
    void findMyTeams_許可されていないsortならBadRequestExceptionになりRepositoryは呼ばれない() {
        Pageable pageable = PageRequest.of(0, 20, Sort.by("foo"));

        assertThrows(BadRequestException.class, () -> teamService.findMyTeams(1L, null, pageable));

        verify(teamUserRepository, never()).findTeams(any(), any(), any());
    }

    @Test
    void findMyTeams_許可されたsortは置き換えられt_idが最後に足される() {
        Pageable pageable = PageRequest.of(2, 10, Sort.by(Sort.Direction.DESC, "createdAt"));

        teamService.findMyTeams(1L, null, pageable);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(teamUserRepository).findTeams(eq(1L), any(), pageableCaptor.capture());
        Pageable passed = pageableCaptor.getValue();

        List<Sort.Order> orders = passed.getSort().toList();
        assertEquals(2, orders.size());
        assertEquals(2, passed.getPageNumber());
        assertEquals(10, passed.getPageSize());

        assertEquals("t.createdAt", orders.get(0).getProperty());
        assertEquals(Sort.Direction.DESC, orders.get(0).getDirection());

        assertEquals("t.id", orders.get(1).getProperty());
        assertEquals(Sort.Direction.ASC, orders.get(1).getDirection());
    }

    @Test
    void findMyTeams_sortの指定がなければnameの昇順になりt_idが最後に足される() {
        Pageable pageable = PageRequest.of(2, 10);

        teamService.findMyTeams(1L, null, pageable);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(teamUserRepository).findTeams(eq(1L), any(), pageableCaptor.capture());
        Pageable passed = pageableCaptor.getValue();
        List<Sort.Order> orders = passed.getSort().toList();

        assertEquals(2, orders.size());
        assertEquals(2, passed.getPageNumber());
        assertEquals(10, passed.getPageSize());

        assertEquals("t.name", orders.get(0).getProperty());
        assertEquals(Sort.Direction.ASC, orders.get(0).getDirection());

        assertEquals("t.id", orders.get(1).getProperty());
        assertEquals(Sort.Direction.ASC, orders.get(1).getDirection());
    }

    @Test
    void findTeam_自分がOWNERとして所属しているチームが返る() {
        TeamUser expectedTeamUser = new TeamUser(10L, 1L, TeamRole.OWNER);
        Team expectedTeam = new Team(1L, "hashed", "test_team", "test");
        ReflectionTestUtils.setField(expectedTeam, "id", 10L);

        when(teamUserRepository.findById(new TeamUserId(10L, 1L)))
                .thenReturn(Optional.of(expectedTeamUser));
        when(teamRepository.findById(10L))
                .thenReturn(Optional.of(expectedTeam));

        TeamResponse response = teamService.findTeam(1L, 10L);

        assertEquals(10L, response.id());
        assertEquals("test_team", response.name());
        assertEquals(TeamRole.OWNER, response.role());
    }

    @Test
    void findTeam_チームに所属していない場合はNotFoundExceptionが発生しteamRepositoryのfindByIdが呼ばれない() {
        when(teamUserRepository.findById(new TeamUserId(10L, 1L)))
                .thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> teamService.findTeam(1L, 10L));
        verify(teamRepository, never()).findById(any());
    }

    @Test
    void join_小文字や空白付きのコードも正規化されて検索される() {
        JoinTeamRequest request = new JoinTeamRequest(" abcd2345wxyz ");
        Team expectedTeam = new Team(1L, "hashed", "test_team", "test");
        ReflectionTestUtils.setField(expectedTeam, "id", 10L);
        TeamUser expectedTeamUser = new TeamUser(10L, 1L, TeamRole.MEMBER);

        when(teamCodeHasher.hash("ABCD2345WXYZ"))
                .thenReturn("hashed");
        when(teamRepository.findByCodeHash("hashed"))
                .thenReturn(Optional.of(expectedTeam));
        when(teamUserRepository.existsById(new TeamUserId(10L, 1L)))
                .thenReturn(false);
        when(teamUserRepository.save(any(TeamUser.class)))
                .thenReturn(expectedTeamUser);

        teamService.join(1L, request);
        ArgumentCaptor<TeamUser> teamUserCaptor = ArgumentCaptor.forClass(TeamUser.class);
        verify(teamUserRepository).save(teamUserCaptor.capture());
        TeamUser saved = teamUserCaptor.getValue();

        assertEquals(10L, saved.getId().teamId());
        assertEquals(1L, saved.getId().userId());
        assertEquals(TeamRole.MEMBER, saved.getRole());
    }

    @Test
    void join_コードが一致するチームがなければNotFoundExceptionが発生する() {
        JoinTeamRequest request = new JoinTeamRequest("ABCD2345WXYZ");

        when(teamCodeHasher.hash("ABCD2345WXYZ"))
                .thenReturn("hashed");
        when(teamRepository.findByCodeHash("hashed"))
                .thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> teamService.join(1L, request));
        verify(teamUserRepository, never()).existsById(any());
        verify(teamUserRepository, never()).save(any());
    }

    @Test
    void join_既に参加しているチームならConflictExceptionが発生する() {
        JoinTeamRequest request = new JoinTeamRequest("ABCD2345WXYZ");
        Team expectedTeam = new Team(1L, "hashed", "test_team", "test");
        ReflectionTestUtils.setField(expectedTeam, "id", 10L);

        when(teamCodeHasher.hash("ABCD2345WXYZ"))
                .thenReturn("hashed");
        when(teamRepository.findByCodeHash("hashed"))
                .thenReturn(Optional.of(expectedTeam));
        when(teamUserRepository.existsById(new TeamUserId(10L, 1L)))
                .thenReturn(true);

        assertThrows(ConflictException.class, () -> teamService.join(1L, request));
        verify(teamUserRepository, never()).save(any());
    }

    @Test
    void findMember_メンバーなら対象のメンバー情報が返る() {
        when(teamUserRepository.existsById(new TeamUserId(10L, 1L)))
                .thenReturn(true);
        when(teamUserRepository.findMember(10L, 5L))
                .thenReturn(Optional.of(new TeamMemberResponse(5L, "test", TeamRole.MEMBER, Instant.now())));

        TeamMemberResponse response = teamService.findMember(1L, 10L, 5L);

        assertEquals(5L, response.userId());
        assertEquals("test", response.name());
        assertEquals(TeamRole.MEMBER, response.role());
    }

    @Test
    void findMember_本人がメンバーでない場合はNotFoundExceptionが発生する() {
        when(teamUserRepository.existsById(new TeamUserId(10L, 1L)))
                .thenReturn(false);

        NotFoundException e = assertThrows(NotFoundException.class, () -> teamService.findMember(1L, 10L, 5L));
        assertEquals("チームが存在しません", e.getMessage());
        verify(teamUserRepository, never()).findMember(any(), any());
    }

    @Test
    void findMember_対象メンバーがチームに存在しない場合はNotFoundExceptionが発生する() {
        when(teamUserRepository.existsById(new TeamUserId(10L, 1L)))
                .thenReturn(true);
        when(teamUserRepository.findMember(10L, 5L))
                .thenReturn(Optional.empty());

        NotFoundException e = assertThrows(NotFoundException.class, () -> teamService.findMember(1L, 10L, 5L));
        assertEquals("メンバーが存在しません", e.getMessage());
    }

    @Test
    void findMembers_本人がメンバーでない場合はNotFoundExceptionが発生する() {
        when(teamUserRepository.existsById(new TeamUserId(10L, 1L)))
                .thenReturn(false);

        assertThrows(NotFoundException.class, () -> teamService.findMembers(1L, 10L, PageRequest.of(0, 20)));
        verify(teamUserRepository, never()).findMembers(any(), any());
    }

    @Test
    void findMembers_許可されていないsortならBadRequestExceptionになりRepositoryは呼ばれない() {
        when(teamUserRepository.existsById(new TeamUserId(10L, 1L)))
                .thenReturn(true);

        assertThrows(BadRequestException.class,
                () -> teamService.findMembers(1L, 10L, PageRequest.of(0, 20, Sort.by("foo"))));
        verify(teamUserRepository, never()).findMembers(any(), any());
    }

    @Test
    void findMembers_許可されたsortは置き換えられtu_id_userIdが最後に足される() {
        when(teamUserRepository.existsById(new TeamUserId(10L, 1L)))
                .thenReturn(true);

        teamService.findMembers(1L, 10L, PageRequest.of(2, 10, Sort.by(Sort.Direction.DESC, "joinedAt")));
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(teamUserRepository).findMembers(eq(10L), pageableCaptor.capture());
        Pageable passed = pageableCaptor.getValue();
        List<Sort.Order> orders = passed.getSort().toList();

        assertEquals(2, orders.size());
        assertEquals(2, passed.getPageNumber());
        assertEquals(10, passed.getPageSize());

        assertEquals("tu.createdAt", orders.get(0).getProperty());
        assertEquals(Sort.Direction.DESC, orders.get(0).getDirection());

        assertEquals("tu.id.userId", orders.get(1).getProperty());
        assertEquals(Sort.Direction.ASC, orders.get(1).getDirection());
    }

    @Test
    void findMembers_sortの指定がなければtu_createdAtの昇順になりtu_id_userIdが最後に足される() {
        when(teamUserRepository.existsById(new TeamUserId(10L, 1L)))
                .thenReturn(true);

        teamService.findMembers(1L, 10L, PageRequest.of(2, 10));

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(teamUserRepository).findMembers(eq(10L), pageableCaptor.capture());
        Pageable passed = pageableCaptor.getValue();
        List<Sort.Order> orders = passed.getSort().toList();

        assertEquals(2, orders.size());
        assertEquals(2, passed.getPageNumber());
        assertEquals(10, passed.getPageSize());

        assertEquals("tu.createdAt", orders.get(0).getProperty());
        assertEquals(Sort.Direction.ASC, orders.get(0).getDirection());

        assertEquals("tu.id.userId", orders.get(1).getProperty());
        assertEquals(Sort.Direction.ASC, orders.get(1).getDirection());
    }

}

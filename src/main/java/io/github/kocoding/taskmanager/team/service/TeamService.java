package io.github.kocoding.taskmanager.team.service;

import java.util.Locale;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.kocoding.taskmanager.common.exception.BadRequestException;
import io.github.kocoding.taskmanager.common.exception.ConflictException;
import io.github.kocoding.taskmanager.common.exception.ForbiddenException;
import io.github.kocoding.taskmanager.common.exception.NotFoundException;
import io.github.kocoding.taskmanager.common.query.QueryUtils;
import io.github.kocoding.taskmanager.team.dto.CreateTeamRequest;
import io.github.kocoding.taskmanager.team.dto.CreateTeamResponse;
import io.github.kocoding.taskmanager.team.dto.JoinTeamRequest;
import io.github.kocoding.taskmanager.team.dto.TeamMemberResponse;
import io.github.kocoding.taskmanager.team.dto.TeamResponse;
import io.github.kocoding.taskmanager.team.dto.UpdateMemberRoleRequest;
import io.github.kocoding.taskmanager.team.dto.UpdateTeamRequest;
import io.github.kocoding.taskmanager.team.entity.Team;
import io.github.kocoding.taskmanager.team.entity.TeamRole;
import io.github.kocoding.taskmanager.team.entity.TeamUser;
import io.github.kocoding.taskmanager.team.entity.TeamUserId;
import io.github.kocoding.taskmanager.team.repository.TeamRepository;
import io.github.kocoding.taskmanager.team.repository.TeamUserRepository;

@Service
public class TeamService {

    // 許可リスト(クライアントの名前 → JPQLの式)は定数で持つ
    private static final Map<String, String> TEAM_SORT_ALLOWLIST = Map.of("name", "t.name", "createdAt", "t.createdAt");
    private static final Map<String, String> MEMBER_SORT_ALLOWLIST = Map.of("name", "u.name", "joinedAt",
            "tu.createdAt");

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

    @Transactional(readOnly = true)
    public Page<TeamResponse> findMyTeams(Long userId, String keyword, Pageable pageable) {
        String pattern = QueryUtils.toLikePattern(keyword);
        Pageable safePageable = QueryUtils.toSafePageable(pageable, TEAM_SORT_ALLOWLIST, "t.name", Sort.Direction.ASC,
                "t.id");
        return teamUserRepository.findTeams(userId, pattern, safePageable);
    }

    @Transactional(readOnly = true)
    public TeamResponse findTeam(Long userId, Long teamId) {
        String message = "チームが存在しません";
        TeamUser teamUser = teamUserRepository.findById(new TeamUserId(teamId, userId))
                .orElseThrow(() -> new NotFoundException(message));
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new NotFoundException(message));
        return TeamResponse.from(team, teamUser.getRole());
    }

    @Transactional
    public TeamResponse join(Long userId, JoinTeamRequest request) {
        String code = request.code().strip().toUpperCase(Locale.ROOT);
        String codeHash = teamCodeHasher.hash(code);

        Team team = teamRepository.findByCodeHash(codeHash)
                .orElseThrow(() -> new NotFoundException("チームコードが正しくありません"));

        if (teamUserRepository.existsById(new TeamUserId(team.getId(), userId))) {
            // 参加済みならConflictException
            throw new ConflictException("既に参加済みのチームです");
        }

        TeamUser teamUser = teamUserRepository.save(new TeamUser(team.getId(), userId, TeamRole.MEMBER));
        return TeamResponse.from(team, teamUser.getRole());
    }

    @Transactional(readOnly = true)
    public TeamMemberResponse findMember(Long userId, Long teamId, Long targetUserId) {
        if (!teamUserRepository.existsById(new TeamUserId(teamId, userId))) {
            throw new NotFoundException("チームが存在しません");
        }

        return teamUserRepository.findMember(teamId, targetUserId)
                .orElseThrow(() -> new NotFoundException("メンバーが存在しません"));
    }

    @Transactional(readOnly = true)
    public Page<TeamMemberResponse> findMembers(Long userId, Long teamId, Pageable pageable) {
        if (!teamUserRepository.existsById(new TeamUserId(teamId, userId))) {
            throw new NotFoundException("チームが存在しません");
        }
        Pageable safePageable = QueryUtils.toSafePageable(pageable, MEMBER_SORT_ALLOWLIST, "tu.createdAt",
                Sort.Direction.ASC, "tu.id.userId");
        return teamUserRepository.findMembers(teamId, safePageable);
    }

    @Transactional
    public TeamResponse update(Long userId, Long teamId, UpdateTeamRequest request) {
        TeamUser teamUser = teamUserRepository.findById(new TeamUserId(teamId, userId))
                .orElseThrow(() -> new NotFoundException("チームが存在しません"));

        if (teamUser.getRole() != TeamRole.OWNER) {
            throw new ForbiddenException("チームを更新する権限がありません");
        }

        if (request.getName() != null && request.getName().isEmpty()) {
            throw new BadRequestException("チーム名にnullは指定できません");
        }

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new NotFoundException("チームが存在しません"));

        if (request.getName() != null) {
            team.rename(request.getName().get());
        }

        if (request.getDescription() != null) {
            team.changeDescription(request.getDescription().orElse(null));
        }

        return TeamResponse.from(team, teamUser.getRole());
    }

    @Transactional
    public TeamMemberResponse updateRole(Long userId, Long teamId, Long targetUserId, UpdateMemberRoleRequest request) {
        TeamUser teamUser = teamUserRepository.findById(new TeamUserId(teamId, userId))
                .orElseThrow(() -> new NotFoundException("チームが存在しません"));

        if (teamUser.getRole() != TeamRole.OWNER) {
            throw new ForbiddenException("ロールを更新する権限がありません");
        }

        if (request.role() != TeamRole.OWNER) {
            throw new BadRequestException("OWNER以外は指定できません");
        }

        TeamUser target = teamUserRepository.findById(new TeamUserId(teamId, targetUserId))
                .orElseThrow(() -> new NotFoundException("メンバーが存在しません"));

        if (target.getRole() == TeamRole.OWNER) {
            throw new ConflictException("既にOWNERです");
        }

        target.changeRole(request.role());

        return teamUserRepository.findMember(teamId, targetUserId)
                .orElseThrow(() -> new NotFoundException("メンバーが存在しません"));
    }

}

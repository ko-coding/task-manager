package io.github.kocoding.taskmanager.team.service;

import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.JpaSort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.kocoding.taskmanager.common.exception.BadRequestException;
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

    // 許可リスト(クライアントの名前 → JPQLの式)は定数で持つ
    private static final Map<String, String> allowList = Map.of("name", "t.name", "createdAt", "t.createdAt");

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
        String pattern = toLikePattern(keyword);
        Pageable safePageable = toSafePageable(pageable);
        return teamUserRepository.findTeams(userId, pattern, safePageable);
    }

    private Pageable toSafePageable(Pageable pageable) {
        Sort safeSort = Sort.unsorted();
        Sort sort = pageable.getSort();

        // 1. pageable.getSort() の各 Order を、許可リストと照らし合わせる
        for (Sort.Order order : sort) {
            if (!allowList.containsKey(order.getProperty())) {
                // なければ BadRequestException
                throw new BadRequestException("sortに指定できない項目です:" + order.getProperty());
            }
            // 2. 許可されたものは JpaSort.unsafe(向き, "t.name") などに変換
            safeSort = safeSort.and(JpaSort.unsafe(order.getDirection(), allowList.get(order.getProperty())));
        }

        // 3. sortの指定がなければ name の昇順
        if (safeSort.isUnsorted()) {
            safeSort = safeSort.and(JpaSort.unsafe(Sort.Direction.ASC, "t.name"));
        }

        // 4. 最後に t.id の昇順を必ず足す
        safeSort = safeSort.and(JpaSort.unsafe(Sort.Direction.ASC, "t.id"));

        // 5. PageRequest.of(ページ番号, size, sort) で返す
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), safeSort);
    }

    private String toLikePattern(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return "%";
        }
        keyword = keyword.replace("\\", "\\\\");
        keyword = keyword.replace("%", "\\%");
        keyword = keyword.replace("_", "\\_");

        return "%" + keyword + "%";
    }

}

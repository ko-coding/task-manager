package io.github.kocoding.taskmanager.team.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.github.kocoding.taskmanager.team.dto.CreateTeamRequest;
import io.github.kocoding.taskmanager.team.dto.CreateTeamResponse;
import io.github.kocoding.taskmanager.team.dto.JoinTeamRequest;
import io.github.kocoding.taskmanager.team.dto.TeamMemberResponse;
import io.github.kocoding.taskmanager.team.dto.TeamResponse;
import io.github.kocoding.taskmanager.team.dto.UpdateMemberRoleRequest;
import io.github.kocoding.taskmanager.team.dto.UpdateTeamRequest;
import io.github.kocoding.taskmanager.team.service.TeamService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/teams")
public class TeamController {

    private final TeamService teamService;

    public TeamController(TeamService teamService) {
        this.teamService = teamService;
    }

    @PostMapping
    public ResponseEntity<CreateTeamResponse> createTeam(@AuthenticationPrincipal Long userId,
            @Valid @RequestBody CreateTeamRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(teamService.create(userId, request));
    }

    @GetMapping
    public PagedModel<TeamResponse> list(@AuthenticationPrincipal Long userId,
            @RequestParam(required = false) String keyword,
            Pageable pageable) {
        Page<TeamResponse> page = teamService.findMyTeams(userId, keyword, pageable);
        return new PagedModel<>(page);
    }

    @GetMapping("/{teamId}")
    public ResponseEntity<TeamResponse> getTeam(@AuthenticationPrincipal Long userId, @PathVariable Long teamId) {
        return ResponseEntity.ok(teamService.findTeam(userId, teamId));
    }

    @PostMapping("/join")
    public ResponseEntity<TeamResponse> joinTeam(@AuthenticationPrincipal Long userId,
            @Valid @RequestBody JoinTeamRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(teamService.join(userId, request));
    }

    @GetMapping("/{teamId}/members")
    public PagedModel<TeamMemberResponse> getMembers(@AuthenticationPrincipal Long userId, @PathVariable Long teamId,
            Pageable pageable) {
        Page<TeamMemberResponse> page = teamService.findMembers(userId, teamId, pageable);
        return new PagedModel<>(page);
    }

    @GetMapping("/{teamId}/members/{userId}")
    public ResponseEntity<TeamMemberResponse> getMember(@AuthenticationPrincipal Long userId, @PathVariable Long teamId,
            @PathVariable("userId") Long targetUserId) {
        return ResponseEntity.ok(teamService.findMember(userId, teamId, targetUserId));
    }

    @PatchMapping("/{teamId}")
    public ResponseEntity<TeamResponse> updateTeam(@AuthenticationPrincipal Long userId, @PathVariable Long teamId,
            @Valid @RequestBody UpdateTeamRequest request) {
        return ResponseEntity.ok(teamService.update(userId, teamId, request));
    }

    @PatchMapping("/{teamId}/members/{userId}/role")
    public ResponseEntity<TeamMemberResponse> updateRole(@AuthenticationPrincipal Long userId,
            @PathVariable Long teamId, @PathVariable("userId") Long targetUserId,
            @Valid @RequestBody UpdateMemberRoleRequest request) {
        return ResponseEntity.ok(teamService.updateRole(userId, teamId, targetUserId, request));
    }

}

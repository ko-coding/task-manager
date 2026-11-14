package io.github.kocoding.taskmanager.team.dto;

public record CreateTeamResponse(
        TeamResponse team,
        String code) {
}

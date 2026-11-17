package io.github.kocoding.taskmanager.team.dto;

import jakarta.validation.constraints.NotBlank;

public record JoinTeamRequest(
        @NotBlank String code) {
}

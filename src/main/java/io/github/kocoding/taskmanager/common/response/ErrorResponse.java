package io.github.kocoding.taskmanager.common.response;

import java.util.List;

public record ErrorResponse(int status, String message, List<FieldErrorDetail> errors) {

    public ErrorResponse(int status, String message) {
        this(status, message, List.of());
    }

    public record FieldErrorDetail(String field, String message) {

    }

}

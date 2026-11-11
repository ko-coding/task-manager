package io.github.kocoding.taskmanager.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import io.github.kocoding.taskmanager.common.response.ErrorResponse;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflict(ConflictException e) {
        int status = HttpStatus.CONFLICT.value();
        ErrorResponse body = new ErrorResponse(status, e.getMessage());
        return ResponseEntity.status(status).body(body);
    }

}

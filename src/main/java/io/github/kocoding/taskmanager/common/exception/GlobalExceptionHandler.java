package io.github.kocoding.taskmanager.common.exception;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import io.github.kocoding.taskmanager.common.response.ErrorResponse;
import io.github.kocoding.taskmanager.common.response.ErrorResponse.FieldErrorDetail;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e) {
        int status = HttpStatus.BAD_REQUEST.value();
        List<FieldErrorDetail> errors = e.getBindingResult().getFieldErrors().stream()
                .map(error -> new FieldErrorDetail(error.getField(), error.getDefaultMessage()))
                .toList();

        ErrorResponse body = new ErrorResponse(status, "入力内容に誤りがあります", errors);
        return ResponseEntity.status(status).body(body);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflict(ConflictException e) {
        int status = HttpStatus.CONFLICT.value();
        ErrorResponse body = new ErrorResponse(status, e.getMessage());
        return ResponseEntity.status(status).body(body);
    }

}

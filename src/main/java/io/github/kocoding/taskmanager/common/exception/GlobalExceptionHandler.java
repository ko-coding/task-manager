package io.github.kocoding.taskmanager.common.exception;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
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

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(BadRequestException e) {
        int status = HttpStatus.BAD_REQUEST.value();
        ErrorResponse body = new ErrorResponse(status, e.getMessage());
        return ResponseEntity.status(status).body(body);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleNotReadable(HttpMessageNotReadableException e) {
        int status = HttpStatus.BAD_REQUEST.value();
        ErrorResponse body = new ErrorResponse(status, "リクエストの形式が正しくありません");
        return ResponseEntity.status(status).body(body);
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ErrorResponse> handleForbidden(ForbiddenException e) {
        int status = HttpStatus.FORBIDDEN.value();
        ErrorResponse body = new ErrorResponse(status, e.getMessage());
        return ResponseEntity.status(status).body(body);
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NotFoundException e) {
        int status = HttpStatus.NOT_FOUND.value();
        ErrorResponse body = new ErrorResponse(status, e.getMessage());
        return ResponseEntity.status(status).body(body);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflict(ConflictException e) {
        int status = HttpStatus.CONFLICT.value();
        ErrorResponse body = new ErrorResponse(status, e.getMessage());
        return ResponseEntity.status(status).body(body);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorized(UnauthorizedException e) {
        int status = HttpStatus.UNAUTHORIZED.value();
        ErrorResponse body = new ErrorResponse(status, e.getMessage());
        return ResponseEntity.status(status).body(body);
    }

}

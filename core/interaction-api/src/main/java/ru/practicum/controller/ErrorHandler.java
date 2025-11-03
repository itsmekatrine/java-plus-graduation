package ru.practicum.controller;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import ru.practicum.dto.ApiError;
import ru.practicum.exception.BadRequestException;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.ForbiddenException;
import ru.practicum.exception.NotFoundException;

import java.time.LocalDateTime;

@Slf4j
@RestControllerAdvice
public class ErrorHandler {

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiError> handleNotFoundException(NotFoundException e) {
        log.info("404 {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiError.builder()
                .status(HttpStatus.NOT_FOUND.name())
                .reason("The required object was not found.")
                .message(e.getMessage())
                .timestamp(LocalDateTime.now())
                .build());
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiError> handleBusinessConflict(ConflictException e) {
        log.info("409 (business) {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiError.builder()
                .status(HttpStatus.CONFLICT.name())
                .reason("For the requested operation the conditions are not met.")
                .message(e.getMessage())
                .timestamp(LocalDateTime.now())
                .build());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleSqlConflict(DataIntegrityViolationException e) {
        log.info("409 (sql) {}", e.getMessage());
        String msg = (e.getMostSpecificCause() != null)
                ? e.getMostSpecificCause().getMessage()
                : e.getMessage();
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiError.builder()
                .status(HttpStatus.CONFLICT.name())
                .reason("Integrity constraint has been violated.")
                .message(msg)
                .timestamp(LocalDateTime.now())
                .build());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraint(ConstraintViolationException e) {
        log.info("400 {}", e.getMessage());
        return ResponseEntity.badRequest().body(ApiError.builder()
                .status(HttpStatus.BAD_REQUEST.name())
                .reason("Incorrectly made request.")
                .message(e.getMessage())
                .timestamp(LocalDateTime.now())
                .build());
    }

    @ExceptionHandler({MissingServletRequestParameterException.class,
            MethodArgumentNotValidException.class,
            MethodArgumentTypeMismatchException.class,
            HttpMessageNotReadableException.class,
            BadRequestException.class})
    public ResponseEntity<ApiError> handleBadRequestException(Exception e) {
        log.info("400 {}", e.getMessage());
        return ResponseEntity.badRequest().body(ApiError.builder()
                .status(HttpStatus.BAD_REQUEST.name())
                .reason("Incorrectly made request.")
                .message(e.getMessage())
                .timestamp(LocalDateTime.now())
                .build());
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ApiError> handleForbidden(ForbiddenException e) {
        log.info("403 {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiError.builder()
                .status(HttpStatus.FORBIDDEN.name())
                .reason("Access denied.")
                .message(e.getMessage())
                .timestamp(LocalDateTime.now())
                .build());
    }

    @ExceptionHandler(feign.FeignException.class)
    public ResponseEntity<ApiError> handleFeign(feign.FeignException e) {
        int raw = e.status();
        HttpStatus status = (raw >= 100 && raw < 600) ? HttpStatus.valueOf(raw) : HttpStatus.BAD_GATEWAY;
        log.info("{} (feign) {}", status.value(), e.getMessage());
        ApiError body = ApiError.builder()
                .status(status.name())
                .reason("Upstream request failed.")
                .message(e.getMessage())
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.status(status).body(body);
    }
}

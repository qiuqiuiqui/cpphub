package com.cpphub.blog.common;

import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;


@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<Result<?>> handleValidation(MethodArgumentNotValidException ex) {
    String msg = ex.getBindingResult()
                     .getFieldErrors()
                     .stream()
                     .map(FieldError::getDefaultMessage)
                     .collect(Collectors.joining("; "));
    return ResponseEntity.badRequest().body(Result.fail(400, msg));
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<Result<?>> handleIllegalArgument(IllegalArgumentException ex) {
    return ResponseEntity.badRequest().body(Result.fail(400, ex.getMessage()));
  }

  @ExceptionHandler(BadCredentialsException.class)
  public ResponseEntity<Result<?>> handleBadCredentials(BadCredentialsException ex) {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        .body(Result.fail(401, "Invalid username or password"));
  }

  @ExceptionHandler(AuthenticationException.class)
  public ResponseEntity<Result<?>> handleAuth(AuthenticationException ex) {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Result.fail(401, ex.getMessage()));
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<Result<?>> handleAccessDenied(AccessDeniedException ex) {
    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Result.fail(403, "Access denied"));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<Result<?>> handleAll(Exception ex) {
    log.error("Unhandled exception", ex);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(Result.fail(500, "Internal server error: " + ex.getMessage()));
  }
}
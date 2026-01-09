package com.myqyl.aitradex.api.controller;

import com.myqyl.aitradex.api.dto.ErrorResponse;
import com.myqyl.aitradex.etrade.exception.EtradeApiException;
import com.myqyl.aitradex.exception.NotFoundException;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

@ControllerAdvice
public class ApiExceptionHandler {

  @ExceptionHandler(NotFoundException.class)
  public ResponseEntity<ErrorResponse> handleNotFound(NotFoundException ex, WebRequest request) {
    ErrorResponse response =
        new ErrorResponse(
            "NOT_FOUND",
            ex.getMessage(),
            HttpStatus.NOT_FOUND.value(),
            OffsetDateTime.now(),
            Collections.emptyList());
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleValidationErrors(
      MethodArgumentNotValidException ex, WebRequest request) {
    List<String> details =
        ex.getBindingResult().getFieldErrors().stream()
            .map(ApiExceptionHandler::formatFieldError)
            .toList();
    ErrorResponse response =
        new ErrorResponse(
            "VALIDATION_ERROR",
            "Validation failed",
            HttpStatus.BAD_REQUEST.value(),
            OffsetDateTime.now(),
            details);
    return ResponseEntity.badRequest().body(response);
  }

  @ExceptionHandler(EtradeApiException.class)
  public ResponseEntity<ErrorResponse> handleEtradeApiException(EtradeApiException ex, WebRequest request) {
    ErrorResponse response =
        new ErrorResponse(
            ex.getErrorCode(),
            ex.getErrorMessage(),
            ex.getHttpStatus(),
            OffsetDateTime.now(),
            Collections.emptyList());
    return ResponseEntity.status(ex.getHttpStatus()).body(response);
  }

  private static String formatFieldError(FieldError error) {
    String field = error.getField();
    String defaultMessage = error.getDefaultMessage();
    return "%s: %s".formatted(field, defaultMessage == null ? "invalid value" : defaultMessage);
  }
}

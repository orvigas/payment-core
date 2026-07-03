package com.payment.errors;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Global exception handler for REST endpoints.
 *
 * <p>Centralizes exception handling across all REST controllers, translating application-specific
 * exceptions into appropriate HTTP responses with structured error information.
 *
 * @author Orlando Villegas (orvigas@gmail.com)
 * @version 1.0.0
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

  /**
   * Handles PaymentNotFoundException.
   *
   * @param ex the payment not found exception
   * @return ResponseEntity with HTTP 404 (Not Found) status and error details
   */
  @ExceptionHandler(PaymentNotFoundException.class)
  public ResponseEntity<Map<String, Object>> handlePaymentNotFound(PaymentNotFoundException ex) {
    log.warn("Payment not found: {}", ex.getMessage());
    return buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage());
  }

  /**
   * Handles InvalidPaymentException.
   *
   * @param ex the invalid payment exception
   * @return ResponseEntity with HTTP 400 (Bad Request) status and error details
   */
  @ExceptionHandler(InvalidPaymentException.class)
  public ResponseEntity<Map<String, Object>> handleInvalidPayment(InvalidPaymentException ex) {
    log.warn("Invalid payment: {}", ex.getMessage());
    return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
  }

  /**
   * Handles RateLimitExceededException.
   *
   * @param ex the rate limit exceeded exception
   * @return ResponseEntity with HTTP 429 (Too Many Requests) status and error details
   */
  @ExceptionHandler(RateLimitExceededException.class)
  public ResponseEntity<Map<String, Object>> handleRateLimitExceeded(RateLimitExceededException ex) {
    log.warn("Rate limit exceeded: {}", ex.getMessage());
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("timestamp", LocalDateTime.now());
    body.put("status", 429);
    body.put("error", "Too Many Requests");
    body.put("message", ex.getMessage());
    return new ResponseEntity<>(body, HttpStatus.TOO_MANY_REQUESTS);
  }

  /**
   * Handles all unhandled exceptions.
   *
   * @param ex the exception
   * @return ResponseEntity with HTTP 500 (Internal Server Error) status and error details
   */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
    log.error("Unexpected error", ex);
    return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
  }

  /**
   * Builds a structured error response.
   *
   * @param status HTTP status code
   * @param message error message
   * @return ResponseEntity with error details
   */
  private ResponseEntity<Map<String, Object>> buildErrorResponse(HttpStatus status, String message) {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("timestamp", LocalDateTime.now());
    body.put("status", status.value());
    body.put("error", status.getReasonPhrase());
    body.put("message", message);
    return new ResponseEntity<>(body, status);
  }
}
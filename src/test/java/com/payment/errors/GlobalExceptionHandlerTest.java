package com.payment.errors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link GlobalExceptionHandler}.
 *
 * @author orvigas@gmail.com
 */
class GlobalExceptionHandlerTest {

  private GlobalExceptionHandler exceptionHandler;

  @BeforeEach
  void setUp() {
    exceptionHandler = new GlobalExceptionHandler();
  }

  @Test
  void testHandlePaymentNotFound() {
    PaymentNotFoundException exception = new PaymentNotFoundException("Payment not found: pay_123");

    ResponseEntity<Map<String, Object>> response = exceptionHandler.handlePaymentNotFound(exception);
    Map<String, Object> body = response.getBody();

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    assertNotNull(body);
    assertEquals(404, body.get("status"));
    assertEquals("Not Found", body.get("error"));
    assertEquals("Payment not found: pay_123", body.get("message"));
    assertNotNull(body.get("timestamp"));
  }

  @Test
  void testHandlePaymentNotFoundWithDifferentMessage() {
    PaymentNotFoundException exception = new PaymentNotFoundException("Payment ID: xyz not found");

    ResponseEntity<Map<String, Object>> response = exceptionHandler.handlePaymentNotFound(exception);
    Map<String, Object> body = response.getBody();

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    assertNotNull(body);
    assertEquals("Payment ID: xyz not found", body.get("message"));
  }

  @Test
  void testHandleInvalidPayment() {
    InvalidPaymentException exception = new InvalidPaymentException("Amount is invalid");

    ResponseEntity<Map<String, Object>> response = exceptionHandler.handleInvalidPayment(exception);
    Map<String, Object> body = response.getBody();

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertNotNull(body);
    assertEquals(400, body.get("status"));
    assertEquals("Bad Request", body.get("error"));
    assertEquals("Amount is invalid", body.get("message"));
    assertNotNull(body.get("timestamp"));
  }

  @Test
  void testHandleInvalidPaymentWithDifferentMessage() {
    InvalidPaymentException exception = new InvalidPaymentException("Only PENDING payments can be confirmed");

    ResponseEntity<Map<String, Object>> response = exceptionHandler.handleInvalidPayment(exception);
    Map<String, Object> body = response.getBody();

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertNotNull(body);
    assertEquals("Only PENDING payments can be confirmed", body.get("message"));
  }

  @Test
  void testHandleGenericException() {
    Exception exception = new RuntimeException("Some unexpected error");

    ResponseEntity<Map<String, Object>> response = exceptionHandler.handleGenericException(exception);
    Map<String, Object> body = response.getBody();

    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    assertNotNull(body);
    assertEquals(500, body.get("status"));
    assertEquals("Internal Server Error", body.get("error"));
    assertEquals("An unexpected error occurred", body.get("message"));
    assertNotNull(body.get("timestamp"));
  }

  @Test
  void testHandleGenericExceptionWithNullPointerException() {
    Exception exception = new NullPointerException();

    ResponseEntity<Map<String, Object>> response = exceptionHandler.handleGenericException(exception);
    Map<String, Object> body = response.getBody();

    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    assertNotNull(body);
    assertEquals(500, body.get("status"));
    assertEquals("An unexpected error occurred", body.get("message"));
  }

  @Test
  void testHandleGenericExceptionWithIllegalArgumentException() {
    Exception exception = new IllegalArgumentException("Bad argument");

    ResponseEntity<Map<String, Object>> response = exceptionHandler.handleGenericException(exception);
    Map<String, Object> body = response.getBody();

    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    assertNotNull(body);
    assertEquals("An unexpected error occurred", body.get("message"));
  }

  @Test
  void testErrorResponseStructure() {
    PaymentNotFoundException exception = new PaymentNotFoundException("Test message");

    ResponseEntity<Map<String, Object>> response = exceptionHandler.handlePaymentNotFound(exception);
    Map<String, Object> body = response.getBody();

    assertNotNull(body);
    assertEquals(4, body.size());
    assertTrue(body.containsKey("timestamp"));
    assertTrue(body.containsKey("status"));
    assertTrue(body.containsKey("error"));
    assertTrue(body.containsKey("message"));
  }

  @Test
  void testErrorResponseContainsTimestamp() {
    InvalidPaymentException exception = new InvalidPaymentException("Error");

    ResponseEntity<Map<String, Object>> response = exceptionHandler.handleInvalidPayment(exception);
    Map<String, Object> body = response.getBody();

    assertNotNull(body);
    assertNotNull(body.get("timestamp"));
  }

  @Test
  void testPaymentNotFoundResponseStatus() {
    PaymentNotFoundException exception = new PaymentNotFoundException("Payment not found");

    ResponseEntity<Map<String, Object>> response = exceptionHandler.handlePaymentNotFound(exception);

    assertEquals(404, response.getStatusCode().value());
  }

  @Test
  void testInvalidPaymentResponseStatus() {
    InvalidPaymentException exception = new InvalidPaymentException("Invalid payment");

    ResponseEntity<Map<String, Object>> response = exceptionHandler.handleInvalidPayment(exception);

    assertEquals(400, response.getStatusCode().value());
  }

  @Test
  void testGenericExceptionResponseStatus() {
    Exception exception = new RuntimeException("Error");

    ResponseEntity<Map<String, Object>> response = exceptionHandler.handleGenericException(exception);

    assertEquals(500, response.getStatusCode().value());
  }

  @Test
  void testHandleInvalidCredentials() {
    InvalidCredentialsException exception = new InvalidCredentialsException("Invalid username or password");

    ResponseEntity<Map<String, Object>> response = exceptionHandler.handleInvalidCredentials(exception);
    Map<String, Object> body = response.getBody();

    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    assertNotNull(body);
    assertEquals(401, body.get("status"));
    assertEquals("Unauthorized", body.get("error"));
    assertEquals("Invalid username or password", body.get("message"));
    assertNotNull(body.get("timestamp"));
  }

  @Test
  void testHandleInvalidCredentialsWithDifferentMessage() {
    InvalidCredentialsException exception = new InvalidCredentialsException("Authentication failed");

    ResponseEntity<Map<String, Object>> response = exceptionHandler.handleInvalidCredentials(exception);
    Map<String, Object> body = response.getBody();

    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    assertNotNull(body);
    assertEquals("Authentication failed", body.get("message"));
  }

  @Test
  void testHandleInvalidCredentialsResponseStatus() {
    InvalidCredentialsException exception = new InvalidCredentialsException("Credentials invalid");

    ResponseEntity<Map<String, Object>> response = exceptionHandler.handleInvalidCredentials(exception);

    assertEquals(401, response.getStatusCode().value());
  }

  @Test
  void testHandleRateLimitExceeded() {
    RateLimitExceededException exception = new RateLimitExceededException("Too many login attempts");

    ResponseEntity<Map<String, Object>> response = exceptionHandler.handleRateLimitExceeded(exception);
    Map<String, Object> body = response.getBody();

    assertEquals(HttpStatus.TOO_MANY_REQUESTS, response.getStatusCode());
    assertNotNull(body);
    assertEquals(429, body.get("status"));
    assertEquals("Too Many Requests", body.get("error"));
    assertEquals("Too many login attempts", body.get("message"));
    assertNotNull(body.get("timestamp"));
  }

  @Test
  void testHandleRateLimitExceededWithDifferentMessage() {
    RateLimitExceededException exception = new RateLimitExceededException("Payment creation rate limit exceeded");

    ResponseEntity<Map<String, Object>> response = exceptionHandler.handleRateLimitExceeded(exception);
    Map<String, Object> body = response.getBody();

    assertEquals(HttpStatus.TOO_MANY_REQUESTS, response.getStatusCode());
    assertNotNull(body);
    assertEquals("Payment creation rate limit exceeded", body.get("message"));
  }

  @Test
  void testHandleRateLimitExceededResponseStatus() {
    RateLimitExceededException exception = new RateLimitExceededException("Rate limit exceeded");

    ResponseEntity<Map<String, Object>> response = exceptionHandler.handleRateLimitExceeded(exception);

    assertEquals(429, response.getStatusCode().value());
  }

  @Test
  void testRateLimitExceededResponseStructure() {
    RateLimitExceededException exception = new RateLimitExceededException("Test rate limit");

    ResponseEntity<Map<String, Object>> response = exceptionHandler.handleRateLimitExceeded(exception);
    Map<String, Object> body = response.getBody();

    assertNotNull(body);
    assertEquals(4, body.size());
    assertTrue(body.containsKey("timestamp"));
    assertTrue(body.containsKey("status"));
    assertTrue(body.containsKey("error"));
    assertTrue(body.containsKey("message"));
  }

  @Test
  void testInvalidCredentialsResponseStructure() {
    InvalidCredentialsException exception = new InvalidCredentialsException("Test credentials");

    ResponseEntity<Map<String, Object>> response = exceptionHandler.handleInvalidCredentials(exception);
    Map<String, Object> body = response.getBody();

    assertNotNull(body);
    assertEquals(4, body.size());
    assertTrue(body.containsKey("timestamp"));
    assertTrue(body.containsKey("status"));
    assertTrue(body.containsKey("error"));
    assertTrue(body.containsKey("message"));
  }
}

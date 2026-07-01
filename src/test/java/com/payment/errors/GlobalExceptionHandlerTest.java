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
}

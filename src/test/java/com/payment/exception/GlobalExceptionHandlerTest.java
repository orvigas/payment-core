package com.payment.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

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

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(404, response.getBody().get("status"));
    assertEquals("Not Found", response.getBody().get("error"));
    assertEquals("Payment not found: pay_123", response.getBody().get("message"));
    assertNotNull(response.getBody().get("timestamp"));
  }

  @Test
  void testHandlePaymentNotFoundWithDifferentMessage() {
    PaymentNotFoundException exception = new PaymentNotFoundException("Payment ID: xyz not found");

    ResponseEntity<Map<String, Object>> response = exceptionHandler.handlePaymentNotFound(exception);

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    assertEquals("Payment ID: xyz not found", response.getBody().get("message"));
  }

  @Test
  void testHandleInvalidPayment() {
    InvalidPaymentException exception = new InvalidPaymentException("Amount is invalid");

    ResponseEntity<Map<String, Object>> response = exceptionHandler.handleInvalidPayment(exception);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(400, response.getBody().get("status"));
    assertEquals("Bad Request", response.getBody().get("error"));
    assertEquals("Amount is invalid", response.getBody().get("message"));
    assertNotNull(response.getBody().get("timestamp"));
  }

  @Test
  void testHandleInvalidPaymentWithDifferentMessage() {
    InvalidPaymentException exception = new InvalidPaymentException("Only PENDING payments can be confirmed");

    ResponseEntity<Map<String, Object>> response = exceptionHandler.handleInvalidPayment(exception);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertEquals("Only PENDING payments can be confirmed", response.getBody().get("message"));
  }

  @Test
  void testHandleGenericException() {
    Exception exception = new RuntimeException("Some unexpected error");

    ResponseEntity<Map<String, Object>> response = exceptionHandler.handleGenericException(exception);

    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(500, response.getBody().get("status"));
    assertEquals("Internal Server Error", response.getBody().get("error"));
    assertEquals("An unexpected error occurred", response.getBody().get("message"));
    assertNotNull(response.getBody().get("timestamp"));
  }

  @Test
  void testHandleGenericExceptionWithNullPointerException() {
    Exception exception = new NullPointerException();

    ResponseEntity<Map<String, Object>> response = exceptionHandler.handleGenericException(exception);

    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    assertEquals(500, response.getBody().get("status"));
    assertEquals("An unexpected error occurred", response.getBody().get("message"));
  }

  @Test
  void testHandleGenericExceptionWithIllegalArgumentException() {
    Exception exception = new IllegalArgumentException("Bad argument");

    ResponseEntity<Map<String, Object>> response = exceptionHandler.handleGenericException(exception);

    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    assertEquals("An unexpected error occurred", response.getBody().get("message"));
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

    assertNotNull(response.getBody().get("timestamp"));
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

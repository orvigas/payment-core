package com.payment.errors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link ErrorResponse}.
 *
 * @author orvigas@gmail.com
 */
class ErrorResponseTest {

  @Test
  void testAccessorsReturnConstructorValues() {
    LocalDateTime timestamp = LocalDateTime.of(2026, 7, 1, 14, 0);
    ErrorResponse response = new ErrorResponse(timestamp, 404, "Not Found", "Payment with ID 123 not found");

    assertEquals(timestamp, response.timestamp());
    assertEquals(404, response.status());
    assertEquals("Not Found", response.error());
    assertEquals("Payment with ID 123 not found", response.message());
  }

  @Test
  void testEqualityIsValueBased() {
    LocalDateTime timestamp = LocalDateTime.of(2026, 7, 1, 14, 0);
    ErrorResponse first = new ErrorResponse(timestamp, 400, "Bad Request", "Invalid amount");
    ErrorResponse second = new ErrorResponse(timestamp, 400, "Bad Request", "Invalid amount");
    ErrorResponse different = new ErrorResponse(timestamp, 404, "Not Found", "Missing payment");

    assertEquals(first, second);
    assertEquals(first.hashCode(), second.hashCode());
    assertNotEquals(first, different);
  }
}

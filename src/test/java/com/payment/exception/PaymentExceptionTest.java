package com.payment.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PaymentExceptionTest {

  @Test
  void testPaymentNotFoundExceptionWithMessage() {
    String message = "Payment not found";
    PaymentNotFoundException exception = new PaymentNotFoundException(message);

    assertEquals(message, exception.getMessage());
  }

  @Test
  void testPaymentNotFoundExceptionIsRuntimeException() {
    PaymentNotFoundException exception = new PaymentNotFoundException("error");
    assertTrue(exception instanceof RuntimeException);
  }

  @Test
  void testInvalidPaymentExceptionWithMessage() {
    String message = "Invalid payment";
    InvalidPaymentException exception = new InvalidPaymentException(message);

    assertEquals(message, exception.getMessage());
  }

  @Test
  void testInvalidPaymentExceptionIsRuntimeException() {
    InvalidPaymentException exception = new InvalidPaymentException("error");
    assertTrue(exception instanceof RuntimeException);
  }

  @Test
  void testPaymentNotFoundExceptionEmptyMessage() {
    PaymentNotFoundException exception = new PaymentNotFoundException("");
    assertEquals("", exception.getMessage());
  }

  @Test
  void testInvalidPaymentExceptionEmptyMessage() {
    InvalidPaymentException exception = new InvalidPaymentException("");
    assertEquals("", exception.getMessage());
  }

  @Test
  void testPaymentNotFoundExceptionNullMessage() {
    PaymentNotFoundException exception = new PaymentNotFoundException(null);
    assertNull(exception.getMessage());
  }

  @Test
  void testInvalidPaymentExceptionNullMessage() {
    InvalidPaymentException exception = new InvalidPaymentException(null);
    assertNull(exception.getMessage());
  }

  @Test
  void testPaymentNotFoundExceptionThrowable() {
    PaymentNotFoundException exception = new PaymentNotFoundException("Error message");
    assertThrows(PaymentNotFoundException.class, () -> {
      throw exception;
    });
  }

  @Test
  void testInvalidPaymentExceptionThrowable() {
    InvalidPaymentException exception = new InvalidPaymentException("Error message");
    assertThrows(InvalidPaymentException.class, () -> {
      throw exception;
    });
  }

  @Test
  void testPaymentNotFoundExceptionStack() {
    try {
      throw new PaymentNotFoundException("Test");
    } catch (PaymentNotFoundException e) {
      assertNotNull(e.getStackTrace());
      assertTrue(e.getStackTrace().length > 0);
    }
  }

  @Test
  void testInvalidPaymentExceptionStack() {
    try {
      throw new InvalidPaymentException("Test");
    } catch (InvalidPaymentException e) {
      assertNotNull(e.getStackTrace());
      assertTrue(e.getStackTrace().length > 0);
    }
  }
}

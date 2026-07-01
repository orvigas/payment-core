package com.payment.models;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PaymentStatusTest {

  @Test
  void testPaymentStatusValues() {
    PaymentStatus[] statuses = PaymentStatus.values();
    assertEquals(5, statuses.length);
  }

  @Test
  void testPaymentStatusPending() {
    assertEquals("PENDING", PaymentStatus.PENDING.name());
  }

  @Test
  void testPaymentStatusProcessing() {
    assertEquals("PROCESSING", PaymentStatus.PROCESSING.name());
  }

  @Test
  void testPaymentStatusCompleted() {
    assertEquals("COMPLETED", PaymentStatus.COMPLETED.name());
  }

  @Test
  void testPaymentStatusFailed() {
    assertEquals("FAILED", PaymentStatus.FAILED.name());
  }

  @Test
  void testPaymentStatusRefunded() {
    assertEquals("REFUNDED", PaymentStatus.REFUNDED.name());
  }

  @Test
  void testPaymentStatusValueOf() {
    assertEquals(PaymentStatus.PENDING, PaymentStatus.valueOf("PENDING"));
    assertEquals(PaymentStatus.PROCESSING, PaymentStatus.valueOf("PROCESSING"));
    assertEquals(PaymentStatus.COMPLETED, PaymentStatus.valueOf("COMPLETED"));
    assertEquals(PaymentStatus.FAILED, PaymentStatus.valueOf("FAILED"));
    assertEquals(PaymentStatus.REFUNDED, PaymentStatus.valueOf("REFUNDED"));
  }

  @Test
  void testPaymentStatusOrdinal() {
    assertEquals(0, PaymentStatus.PENDING.ordinal());
    assertEquals(1, PaymentStatus.PROCESSING.ordinal());
    assertEquals(2, PaymentStatus.COMPLETED.ordinal());
    assertEquals(3, PaymentStatus.FAILED.ordinal());
    assertEquals(4, PaymentStatus.REFUNDED.ordinal());
  }

  @Test
  void testAllStatusesPresent() {
    assertTrue(PaymentStatus.values().length > 0);
    boolean hasPending = false;
    boolean hasProcessing = false;
    boolean hasCompleted = false;
    boolean hasFailed = false;
    boolean hasRefunded = false;

    for (PaymentStatus status : PaymentStatus.values()) {
      if (status == PaymentStatus.PENDING) hasPending = true;
      if (status == PaymentStatus.PROCESSING) hasProcessing = true;
      if (status == PaymentStatus.COMPLETED) hasCompleted = true;
      if (status == PaymentStatus.FAILED) hasFailed = true;
      if (status == PaymentStatus.REFUNDED) hasRefunded = true;
    }

    assertTrue(hasPending);
    assertTrue(hasProcessing);
    assertTrue(hasCompleted);
    assertTrue(hasFailed);
    assertTrue(hasRefunded);
  }
}

package com.payment.models;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class PaymentTest {

  private Payment payment;

  @BeforeEach
  void setUp() {
    payment = new Payment();
  }

  @Test
  void testPaymentIdGetterSetter() {
    payment.setPaymentId("pay_123");
    assertEquals("pay_123", payment.getPaymentId());
  }

  @Test
  void testUserIdGetterSetter() {
    payment.setUserId("user_456");
    assertEquals("user_456", payment.getUserId());
  }

  @Test
  void testAmountGetterSetter() {
    BigDecimal amount = new BigDecimal("100.50");
    payment.setAmount(amount);
    assertEquals(amount, payment.getAmount());
  }

  @Test
  void testCurrencyGetterSetter() {
    payment.setCurrency("USD");
    assertEquals("USD", payment.getCurrency());
  }

  @Test
  void testMerchantGetterSetter() {
    payment.setMerchant("merchant_123");
    assertEquals("merchant_123", payment.getMerchant());
  }

  @Test
  void testStatusGetterSetter() {
    payment.setStatus(PaymentStatus.PENDING);
    assertEquals(PaymentStatus.PENDING, payment.getStatus());
  }

  @Test
  void testDescriptionGetterSetter() {
    payment.setDescription("Test description");
    assertEquals("Test description", payment.getDescription());
  }

  @Test
  void testCreatedAtGetterSetter() {
    LocalDateTime now = LocalDateTime.now();
    payment.setCreatedAt(now);
    assertEquals(now, payment.getCreatedAt());
  }

  @Test
  void testUpdatedAtGetterSetter() {
    LocalDateTime now = LocalDateTime.now();
    payment.setUpdatedAt(now);
    assertEquals(now, payment.getUpdatedAt());
  }

  @Test
  void testCompletedAtGetterSetter() {
    LocalDateTime now = LocalDateTime.now();
    payment.setCompletedAt(now);
    assertEquals(now, payment.getCompletedAt());
  }

  @Test
  void testPaymentWithAllFieldsSet() {
    LocalDateTime now = LocalDateTime.now();
    BigDecimal amount = new BigDecimal("5000.00");

    payment.setPaymentId("pay_789");
    payment.setUserId("user_789");
    payment.setAmount(amount);
    payment.setCurrency("EUR");
    payment.setMerchant("new-merchant");
    payment.setStatus(PaymentStatus.COMPLETED);
    payment.setDescription("Full payment");
    payment.setCreatedAt(now);
    payment.setUpdatedAt(now);
    payment.setCompletedAt(now);

    assertEquals("pay_789", payment.getPaymentId());
    assertEquals("user_789", payment.getUserId());
    assertEquals(amount, payment.getAmount());
    assertEquals("EUR", payment.getCurrency());
    assertEquals("new-merchant", payment.getMerchant());
    assertEquals(PaymentStatus.COMPLETED, payment.getStatus());
    assertEquals("Full payment", payment.getDescription());
    assertEquals(now, payment.getCreatedAt());
    assertEquals(now, payment.getUpdatedAt());
    assertEquals(now, payment.getCompletedAt());
  }

  @Test
  void testPaymentStatusPending() {
    payment.setStatus(PaymentStatus.PENDING);
    assertEquals(PaymentStatus.PENDING, payment.getStatus());
  }

  @Test
  void testPaymentStatusProcessing() {
    payment.setStatus(PaymentStatus.PROCESSING);
    assertEquals(PaymentStatus.PROCESSING, payment.getStatus());
  }

  @Test
  void testPaymentStatusCompleted() {
    payment.setStatus(PaymentStatus.COMPLETED);
    assertEquals(PaymentStatus.COMPLETED, payment.getStatus());
  }

  @Test
  void testPaymentStatusRefunded() {
    payment.setStatus(PaymentStatus.REFUNDED);
    assertEquals(PaymentStatus.REFUNDED, payment.getStatus());
  }

  @Test
  void testNullFields() {
    payment.setPaymentId(null);
    payment.setUserId(null);
    payment.setAmount(null);
    payment.setCurrency(null);
    payment.setMerchant(null);
    payment.setDescription(null);
    payment.setCreatedAt(null);
    payment.setUpdatedAt(null);
    payment.setCompletedAt(null);

    assertNull(payment.getPaymentId());
    assertNull(payment.getUserId());
    assertNull(payment.getAmount());
    assertNull(payment.getCurrency());
    assertNull(payment.getMerchant());
    assertNull(payment.getDescription());
    assertNull(payment.getCreatedAt());
    assertNull(payment.getUpdatedAt());
    assertNull(payment.getCompletedAt());
  }

  @Test
  void testPaymentStatusAllValues() {
    for (PaymentStatus status : PaymentStatus.values()) {
      payment.setStatus(status);
      assertEquals(status, payment.getStatus());
    }
  }
}

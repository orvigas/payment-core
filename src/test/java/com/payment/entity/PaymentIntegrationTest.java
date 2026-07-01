package com.payment.entity;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class PaymentIntegrationTest {

  @Test
  void testPaymentLifecycleWithCallbacks() {
    Payment payment = new Payment();
    payment.setPaymentId("pay_lifecycle");
    payment.setUserId("user_lifecycle");
    payment.setAmount(new BigDecimal("1000.00"));
    payment.setCurrency("USD");
    payment.setMerchant("lifecycle-merchant");
    payment.setDescription("Lifecycle test");

    // Simulate onCreate callback manually (would be called by JPA PrePersist)
    if (payment.getCreatedAt() == null) {
      payment.setCreatedAt(LocalDateTime.now());
      payment.setUpdatedAt(LocalDateTime.now());
      payment.setStatus(PaymentStatus.PENDING);
    }

    assertEquals(PaymentStatus.PENDING, payment.getStatus());
    assertNotNull(payment.getCreatedAt());
    assertNotNull(payment.getUpdatedAt());
  }

  @Test
  void testPaymentStatusTransitions() {
    Payment payment = new Payment();
    payment.setPaymentId("pay_transitions");
    payment.setStatus(PaymentStatus.PENDING);

    assertEquals(PaymentStatus.PENDING, payment.getStatus());

    payment.setStatus(PaymentStatus.PROCESSING);
    assertEquals(PaymentStatus.PROCESSING, payment.getStatus());

    payment.setStatus(PaymentStatus.COMPLETED);
    LocalDateTime completedTime = LocalDateTime.now();
    payment.setCompletedAt(completedTime);
    assertEquals(PaymentStatus.COMPLETED, payment.getStatus());
    assertEquals(completedTime, payment.getCompletedAt());

    payment.setStatus(PaymentStatus.REFUNDED);
    assertEquals(PaymentStatus.REFUNDED, payment.getStatus());
  }

  @Test
  void testPaymentFieldUpdates() {
    Payment payment = new Payment();
    payment.setPaymentId("pay_updates");
    payment.setAmount(new BigDecimal("100.00"));
    payment.setUserId("original_user");

    assertEquals(new BigDecimal("100.00"), payment.getAmount());
    assertEquals("original_user", payment.getUserId());

    payment.setAmount(new BigDecimal("150.00"));
    payment.setUserId("updated_user");

    assertEquals(new BigDecimal("150.00"), payment.getAmount());
    assertEquals("updated_user", payment.getUserId());
  }

  @Test
  void testPaymentCompletionDetails() {
    Payment payment = new Payment();
    String paymentId = "pay_completion";
    payment.setPaymentId(paymentId);
    payment.setUserId("user_complete");
    payment.setAmount(new BigDecimal("2000.00"));
    payment.setCurrency("EUR");
    payment.setMerchant("premium-merchant");
    payment.setStatus(PaymentStatus.PENDING);
    payment.setDescription("Premium purchase");

    assertEquals(paymentId, payment.getPaymentId());
    assertEquals("user_complete", payment.getUserId());
    assertEquals(new BigDecimal("2000.00"), payment.getAmount());
    assertEquals("EUR", payment.getCurrency());
    assertEquals("premium-merchant", payment.getMerchant());
    assertEquals("Premium purchase", payment.getDescription());

    LocalDateTime completedTime = LocalDateTime.now();
    payment.setStatus(PaymentStatus.COMPLETED);
    payment.setCompletedAt(completedTime);

    assertEquals(PaymentStatus.COMPLETED, payment.getStatus());
    assertEquals(completedTime, payment.getCompletedAt());
  }

  @Test
  void testPaymentRefundFlow() {
    Payment payment = new Payment();
    payment.setPaymentId("pay_refund");
    payment.setAmount(new BigDecimal("500.00"));
    payment.setStatus(PaymentStatus.COMPLETED);
    LocalDateTime completionTime = LocalDateTime.now();
    payment.setCompletedAt(completionTime);

    assertEquals(PaymentStatus.COMPLETED, payment.getStatus());
    assertEquals(new BigDecimal("500.00"), payment.getAmount());
    assertEquals(completionTime, payment.getCompletedAt());

    payment.setStatus(PaymentStatus.REFUNDED);
    assertEquals(PaymentStatus.REFUNDED, payment.getStatus());
  }

  @Test
  void testPaymentTimestampManagement() {
    Payment payment = new Payment();
    LocalDateTime createdTime = LocalDateTime.now();
    LocalDateTime updatedTime = LocalDateTime.now().plusMinutes(5);

    payment.setCreatedAt(createdTime);
    payment.setUpdatedAt(updatedTime);

    assertEquals(createdTime, payment.getCreatedAt());
    assertEquals(updatedTime, payment.getUpdatedAt());
    assertNull(payment.getCompletedAt());

    LocalDateTime completedTime = LocalDateTime.now().plusHours(1);
    payment.setCompletedAt(completedTime);

    assertEquals(createdTime, payment.getCreatedAt());
    assertEquals(updatedTime, payment.getUpdatedAt());
    assertEquals(completedTime, payment.getCompletedAt());
  }

  @Test
  void testPaymentAllCurrencies() {
    String[] currencies = {"USD", "EUR", "GBP", "JPY", "MXN", "CAD", "AUD"};

    for (String currency : currencies) {
      Payment payment = new Payment();
      payment.setCurrency(currency);
      assertEquals(currency, payment.getCurrency());
    }
  }

  @Test
  void testPaymentAllStatuses() {
    Payment payment = new Payment();

    for (PaymentStatus status : PaymentStatus.values()) {
      payment.setStatus(status);
      assertEquals(status, payment.getStatus());
    }
  }
}

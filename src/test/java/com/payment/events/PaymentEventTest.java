package com.payment.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for payment event classes.
 */
@Slf4j
public class PaymentEventTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void testPaymentInitiatedEvent() {
    PaymentInitiatedEvent event = PaymentInitiatedEvent.builder()
        .paymentId("pay_123")
        .userId("user_456")
        .amount(new BigDecimal("5000.00"))
        .currency("MXN")
        .merchant("jersey-mikes")
        .description("Order #123")
        .createdAt(LocalDateTime.now())
        .correlationId(UUID.randomUUID().toString())
        .build();

    assertNotNull(event);
    assertEquals("pay_123", event.getPaymentId());
    assertEquals("user_456", event.getUserId());
    assertEquals(new BigDecimal("5000.00"), event.getAmount());
    assertEquals("MXN", event.getCurrency());
    log.info("Test passed: PaymentInitiatedEvent");
  }

  @Test
  void testPaymentChargedEvent() {
    PaymentChargedEvent event = PaymentChargedEvent.builder()
        .paymentId("pay_123")
        .transactionId(UUID.randomUUID().toString())
        .chargedAt(LocalDateTime.now())
        .success(true)
        .correlationId(UUID.randomUUID().toString())
        .errorMessage(null)
        .build();

    assertNotNull(event);
    assertEquals("pay_123", event.getPaymentId());
    assertTrue(event.isSuccess());
    assertNull(event.getErrorMessage());
    log.info("Test passed: PaymentChargedEvent");
  }

  @Test
  void testPaymentChargedEvent_WithError() {
    PaymentChargedEvent event = PaymentChargedEvent.builder()
        .paymentId("pay_456")
        .transactionId(UUID.randomUUID().toString())
        .chargedAt(LocalDateTime.now())
        .success(false)
        .correlationId(UUID.randomUUID().toString())
        .errorMessage("Card declined")
        .build();

    assertNotNull(event);
    assertEquals("pay_456", event.getPaymentId());
    assertFalse(event.isSuccess());
    assertEquals("Card declined", event.getErrorMessage());
    log.info("Test passed: PaymentChargedEvent with error");
  }

  @Test
  void testPaymentCompletedEvent() {
    PaymentCompletedEvent event = PaymentCompletedEvent.builder()
        .paymentId("pay_123")
        .completedAt(LocalDateTime.now())
        .correlationId(UUID.randomUUID().toString())
        .build();

    assertNotNull(event);
    assertEquals("pay_123", event.getPaymentId());
    assertNotNull(event.getCompletedAt());
    log.info("Test passed: PaymentCompletedEvent");
  }

  @Test
  void testPaymentFailedEvent() {
    PaymentFailedEvent event = PaymentFailedEvent.builder()
        .paymentId("pay_789")
        .failedAt(LocalDateTime.now())
        .reason("Insufficient funds")
        .correlationId(UUID.randomUUID().toString())
        .build();

    assertNotNull(event);
    assertEquals("pay_789", event.getPaymentId());
    assertEquals("Insufficient funds", event.getReason());
    assertNotNull(event.getFailedAt());
    log.info("Test passed: PaymentFailedEvent");
  }

  @Test
  void testPaymentInitiatedEvent_Builder() {
    LocalDateTime now = LocalDateTime.now();
    PaymentInitiatedEvent event = PaymentInitiatedEvent.builder()
        .paymentId("pay_999")
        .userId("user_111")
        .amount(new BigDecimal("1000.00"))
        .currency("USD")
        .merchant("store")
        .description("purchase")
        .createdAt(now)
        .correlationId(UUID.randomUUID().toString())
        .build();

    assertEquals("pay_999", event.getPaymentId());
    assertEquals("user_111", event.getUserId());
    assertEquals(new BigDecimal("1000.00"), event.getAmount());
    assertEquals("USD", event.getCurrency());
    assertEquals("store", event.getMerchant());
    assertEquals("purchase", event.getDescription());
    assertEquals(now, event.getCreatedAt());
    assertNotNull(event.getCorrelationId());
    log.info("Test passed: PaymentInitiatedEvent builder");
  }
}

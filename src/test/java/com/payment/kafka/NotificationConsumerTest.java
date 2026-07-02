package com.payment.kafka;

import com.payment.events.PaymentChargedEvent;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for NotificationConsumer.
 *
 * @author orvigas@gmail.com
 */
@ExtendWith(MockitoExtension.class)
@Slf4j
public class NotificationConsumerTest {

  @InjectMocks
  private NotificationConsumer notificationConsumer;

  private PaymentChargedEvent successEvent;
  private PaymentChargedEvent failureEvent;

  @BeforeEach
  void setUp() {
    successEvent = PaymentChargedEvent.builder()
        .paymentId("pay_123")
        .transactionId(UUID.randomUUID().toString())
        .chargedAt(LocalDateTime.now())
        .success(true)
        .correlationId(UUID.randomUUID().toString())
        .errorMessage(null)
        .build();

    failureEvent = PaymentChargedEvent.builder()
        .paymentId("pay_456")
        .transactionId(UUID.randomUUID().toString())
        .chargedAt(LocalDateTime.now())
        .success(false)
        .correlationId(UUID.randomUUID().toString())
        .errorMessage("Card declined")
        .build();
  }

  @Test
  void testConsumePaymentCharged_Success() {
    assertDoesNotThrow(() -> {
      notificationConsumer.consumePaymentCharged(successEvent, null);
    });
    log.info("Test passed: NotificationConsumer processed success event");
  }

  @Test
  void testConsumePaymentCharged_Failure() {
    assertDoesNotThrow(() -> {
      notificationConsumer.consumePaymentCharged(failureEvent, null);
    });
    log.info("Test passed: NotificationConsumer processed failure event");
  }

  @Test
  void testConsumePaymentCharged_WithCorrelationId() {
    String correlationId = UUID.randomUUID().toString();
    assertDoesNotThrow(() -> {
      notificationConsumer.consumePaymentCharged(successEvent, correlationId);
    });
    log.info("Test passed: NotificationConsumer processed event with correlation ID");
  }

  @Test
  void testConsumePaymentCharged_NullCorrelationId() {
    assertDoesNotThrow(() -> {
      notificationConsumer.consumePaymentCharged(successEvent, null);
    });
    log.info("Test passed: NotificationConsumer handled null correlation ID");
  }

  @Test
  void testConsumePaymentCharged_ProcessingErrorIsRethrown() {
    // Rethrowing lets Kafka's error handling retry or dead-letter the record.
    PaymentChargedEvent brokenEvent = mock(PaymentChargedEvent.class);
    when(brokenEvent.getPaymentId()).thenReturn("pay_789");
    when(brokenEvent.isSuccess()).thenThrow(new IllegalStateException("corrupted payload"));

    RuntimeException thrown = assertThrows(RuntimeException.class, () -> {
      notificationConsumer.consumePaymentCharged(brokenEvent, null);
    });
    assertInstanceOf(IllegalStateException.class, thrown.getCause());
    log.info("Test passed: NotificationConsumer rethrew processing error");
  }
}

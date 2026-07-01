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

/**
 * Unit tests for AnalyticsConsumer.
 */
@ExtendWith(MockitoExtension.class)
@Slf4j
public class AnalyticsConsumerTest {

  @InjectMocks
  private AnalyticsConsumer analyticsConsumer;

  private PaymentChargedEvent successEvent;
  private PaymentChargedEvent failureEvent;

  @BeforeEach
  void setUp() {
    analyticsConsumer = new AnalyticsConsumer();

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
        .errorMessage("Charge failed")
        .build();
  }

  @Test
  void testConsumePaymentCharged_Success() {
    assertEquals(0, analyticsConsumer.getSuccessfulCharges());
    analyticsConsumer.consumePaymentCharged(successEvent);
    assertEquals(1, analyticsConsumer.getSuccessfulCharges());
    log.info("Test passed: AnalyticsConsumer tracked successful charge");
  }

  @Test
  void testConsumePaymentCharged_Failure() {
    assertEquals(0, analyticsConsumer.getFailedCharges());
    analyticsConsumer.consumePaymentCharged(failureEvent);
    assertEquals(1, analyticsConsumer.getFailedCharges());
    log.info("Test passed: AnalyticsConsumer tracked failed charge");
  }

  @Test
  void testConsumePaymentCharged_MultipleSuccesses() {
    assertEquals(0, analyticsConsumer.getSuccessfulCharges());
    analyticsConsumer.consumePaymentCharged(successEvent);
    analyticsConsumer.consumePaymentCharged(successEvent);
    analyticsConsumer.consumePaymentCharged(successEvent);
    assertEquals(3, analyticsConsumer.getSuccessfulCharges());
    log.info("Test passed: AnalyticsConsumer tracked multiple successful charges");
  }

  @Test
  void testConsumePaymentCharged_MultipleFailures() {
    assertEquals(0, analyticsConsumer.getFailedCharges());
    analyticsConsumer.consumePaymentCharged(failureEvent);
    analyticsConsumer.consumePaymentCharged(failureEvent);
    assertEquals(2, analyticsConsumer.getFailedCharges());
    log.info("Test passed: AnalyticsConsumer tracked multiple failed charges");
  }

  @Test
  void testConsumePaymentCharged_MixedResults() {
    analyticsConsumer.consumePaymentCharged(successEvent);
    analyticsConsumer.consumePaymentCharged(failureEvent);
    analyticsConsumer.consumePaymentCharged(successEvent);
    analyticsConsumer.consumePaymentCharged(failureEvent);

    assertEquals(2, analyticsConsumer.getSuccessfulCharges());
    assertEquals(2, analyticsConsumer.getFailedCharges());
    log.info("Test passed: AnalyticsConsumer tracked mixed charge results");
  }

  @Test
  void testGetSuccessfulCharges() {
    assertEquals(0, analyticsConsumer.getSuccessfulCharges());
    analyticsConsumer.consumePaymentCharged(successEvent);
    assertEquals(1, analyticsConsumer.getSuccessfulCharges());
  }

  @Test
  void testGetFailedCharges() {
    assertEquals(0, analyticsConsumer.getFailedCharges());
    analyticsConsumer.consumePaymentCharged(failureEvent);
    assertEquals(1, analyticsConsumer.getFailedCharges());
  }
}

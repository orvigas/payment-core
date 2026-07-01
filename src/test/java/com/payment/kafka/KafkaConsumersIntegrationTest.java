package com.payment.kafka;

import com.payment.events.PaymentChargedEvent;
import com.payment.events.PaymentInitiatedEvent;
import com.payment.models.Payment;
import com.payment.models.PaymentStatus;
import com.payment.repositories.PaymentRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Integration tests for all Kafka consumers working together.
 */
@ExtendWith(MockitoExtension.class)
@Slf4j
public class KafkaConsumersIntegrationTest {

  @Mock
  private PaymentRepository paymentRepository;

  @Mock
  private PaymentProducer paymentProducer;

  @InjectMocks
  private ChargingConsumer chargingConsumer;

  @InjectMocks
  private NotificationConsumer notificationConsumer;

  @InjectMocks
  private AnalyticsConsumer analyticsConsumer;

  private PaymentInitiatedEvent initiatedEvent;
  private PaymentChargedEvent successEvent;
  private PaymentChargedEvent failureEvent;
  private Payment payment;

  @BeforeEach
  void setUp() {
    analyticsConsumer = new AnalyticsConsumer();

    initiatedEvent = PaymentInitiatedEvent.builder()
        .paymentId("pay_full_test")
        .userId("user_full")
        .amount(new BigDecimal("10000.00"))
        .currency("MXN")
        .merchant("test-merchant")
        .description("Full integration test")
        .createdAt(LocalDateTime.now())
        .correlationId(UUID.randomUUID().toString())
        .build();

    successEvent = PaymentChargedEvent.builder()
        .paymentId("pay_full_test")
        .transactionId(UUID.randomUUID().toString())
        .chargedAt(LocalDateTime.now())
        .success(true)
        .correlationId(initiatedEvent.getCorrelationId())
        .errorMessage(null)
        .build();

    failureEvent = PaymentChargedEvent.builder()
        .paymentId("pay_full_test")
        .transactionId(UUID.randomUUID().toString())
        .chargedAt(LocalDateTime.now())
        .success(false)
        .correlationId(initiatedEvent.getCorrelationId())
        .errorMessage("Payment declined")
        .build();

    payment = new Payment();
    payment.setPaymentId(initiatedEvent.getPaymentId());
    payment.setStatus(PaymentStatus.PENDING);
  }

  @Test
  void testFullPaymentFlow_Success() {
    when(paymentRepository.findByPaymentId("pay_full_test")).thenReturn(Optional.of(payment));
    when(paymentRepository.save(any())).thenReturn(payment);

    // Step 1: ChargingConsumer processes initiated event
    chargingConsumer.consumePaymentInitiated(initiatedEvent, null);
    verify(paymentRepository, times(2)).save(any());
    verify(paymentProducer, times(1)).publishPaymentCharged(any());

    // Step 2: NotificationConsumer processes charged event
    assertDoesNotThrow(() -> {
      notificationConsumer.consumePaymentCharged(successEvent, null);
    });

    // Step 3: AnalyticsConsumer tracks the event
    analyticsConsumer.consumePaymentCharged(successEvent);
    assertEquals(1, analyticsConsumer.getSuccessfulCharges());
    assertEquals(0, analyticsConsumer.getFailedCharges());

    log.info("Test passed: Full successful payment flow completed");
  }

  @Test
  void testFullPaymentFlow_Failure() {
    when(paymentRepository.findByPaymentId("pay_full_test")).thenReturn(Optional.of(payment));
    when(paymentRepository.save(any())).thenReturn(payment);

    // Step 1: ChargingConsumer processes initiated event
    chargingConsumer.consumePaymentInitiated(initiatedEvent, null);

    // Step 2: NotificationConsumer processes failed charged event
    assertDoesNotThrow(() -> {
      notificationConsumer.consumePaymentCharged(failureEvent, null);
    });

    // Step 3: AnalyticsConsumer tracks the failed event
    analyticsConsumer.consumePaymentCharged(failureEvent);
    assertEquals(0, analyticsConsumer.getSuccessfulCharges());
    assertEquals(1, analyticsConsumer.getFailedCharges());

    log.info("Test passed: Full failed payment flow completed");
  }

  @Test
  void testMultiplePaymentsFlow() {
    PaymentChargedEvent event1 = PaymentChargedEvent.builder()
        .paymentId("pay_1")
        .transactionId(UUID.randomUUID().toString())
        .chargedAt(LocalDateTime.now())
        .success(true)
        .correlationId(UUID.randomUUID().toString())
        .errorMessage(null)
        .build();

    PaymentChargedEvent event2 = PaymentChargedEvent.builder()
        .paymentId("pay_2")
        .transactionId(UUID.randomUUID().toString())
        .chargedAt(LocalDateTime.now())
        .success(true)
        .correlationId(UUID.randomUUID().toString())
        .errorMessage(null)
        .build();

    PaymentChargedEvent event3 = PaymentChargedEvent.builder()
        .paymentId("pay_3")
        .transactionId(UUID.randomUUID().toString())
        .chargedAt(LocalDateTime.now())
        .success(false)
        .correlationId(UUID.randomUUID().toString())
        .errorMessage("Declined")
        .build();

    analyticsConsumer.consumePaymentCharged(event1);
    analyticsConsumer.consumePaymentCharged(event2);
    analyticsConsumer.consumePaymentCharged(event3);

    assertEquals(2, analyticsConsumer.getSuccessfulCharges());
    assertEquals(1, analyticsConsumer.getFailedCharges());

    log.info("Test passed: Multiple payments flow completed");
  }

  @Test
  void testConsumerErrorHandling() {
    when(paymentRepository.findByPaymentId("pay_full_test")).thenReturn(Optional.empty());

    assertThrows(RuntimeException.class, () -> {
      chargingConsumer.consumePaymentInitiated(initiatedEvent, null);
    });

    log.info("Test passed: Consumer error handling works");
  }

  @Test
  void testAnalyticsConsumerMetrics() {
    analyticsConsumer = new AnalyticsConsumer();

    for (int i = 0; i < 5; i++) {
      PaymentChargedEvent event = PaymentChargedEvent.builder()
          .paymentId("pay_" + i)
          .transactionId(UUID.randomUUID().toString())
          .chargedAt(LocalDateTime.now())
          .success(i % 2 == 0)
          .correlationId(UUID.randomUUID().toString())
          .errorMessage(i % 2 == 0 ? null : "Error")
          .build();

      analyticsConsumer.consumePaymentCharged(event);
    }

    assertEquals(3, analyticsConsumer.getSuccessfulCharges());
    assertEquals(2, analyticsConsumer.getFailedCharges());

    log.info("Test passed: Analytics consumer metrics are accurate");
  }
}

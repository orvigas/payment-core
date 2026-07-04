package com.payment.kafka;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;

import com.payment.events.PaymentChargedEvent;
import com.payment.events.PaymentCompletedEvent;
import com.payment.events.PaymentInitiatedEvent;

/**
 * Unit tests for {@link PaymentProducer}.
 *
 * <p>Verifies topic routing, message headers, correlation ID handling, and that
 * send failures are logged without propagating to the caller.
 *
 * @author orvigas@gmail.com
 */
@ExtendWith(MockitoExtension.class)
class PaymentProducerTest {

  @Mock
  private KafkaTemplate<String, Object> kafkaTemplate;

  @InjectMocks
  private PaymentProducer paymentProducer;

  @Captor
  private ArgumentCaptor<Message<?>> messageCaptor;

  private PaymentInitiatedEvent initiatedEvent;
  private PaymentChargedEvent chargedEvent;
  private PaymentCompletedEvent completedEvent;

  @BeforeEach
  void setUp() {
    initiatedEvent = PaymentInitiatedEvent.builder()
        .paymentId("pay_123")
        .userId("user123")
        .amount(new BigDecimal("100.00"))
        .currency("USD")
        .merchant("test-merchant")
        .createdAt(LocalDateTime.now())
        .build();

    chargedEvent = PaymentChargedEvent.builder()
        .paymentId("pay_123")
        .transactionId("txn_456")
        .chargedAt(LocalDateTime.now())
        .success(true)
        .build();

    completedEvent = PaymentCompletedEvent.builder()
        .paymentId("pay_123")
        .completedAt(LocalDateTime.now())
        .build();
  }

  private void stubSendSuccess() {
    when(kafkaTemplate.send(any(Message.class)))
        .thenReturn(CompletableFuture.completedFuture(null));
  }

  private void stubSendFailure() {
    when(kafkaTemplate.send(any(Message.class)))
        .thenReturn(CompletableFuture.failedFuture(new RuntimeException("broker unavailable")));
  }

  @Test
  void testOnPaymentInitiatedPublishesToKafka() {
    stubSendSuccess();

    paymentProducer.onPaymentInitiated(initiatedEvent);

    verify(kafkaTemplate).send(messageCaptor.capture());
    Message<?> message = messageCaptor.getValue();
    assertEquals(KafkaTopics.PAYMENT_INITIATED, message.getHeaders().get(KafkaHeaders.TOPIC));
    assertEquals("pay_123", message.getHeaders().get(KafkaHeaders.RECEIVED_KEY));
  }

  @Test
  void testPublishPaymentInitiatedGeneratesCorrelationId() {
    stubSendSuccess();

    paymentProducer.publishPaymentInitiated(initiatedEvent);

    verify(kafkaTemplate).send(messageCaptor.capture());
    Message<?> message = messageCaptor.getValue();
    assertEquals(KafkaTopics.PAYMENT_INITIATED, message.getHeaders().get(KafkaHeaders.TOPIC));
    assertEquals("pay_123", message.getHeaders().get(KafkaHeaders.RECEIVED_KEY));
    assertNotNull(initiatedEvent.getCorrelationId());
    assertEquals(initiatedEvent.getCorrelationId(), message.getHeaders().get("correlation_id"));
  }

  @Test
  void testPublishPaymentInitiatedPreservesExistingCorrelationId() {
    stubSendSuccess();
    initiatedEvent.setCorrelationId("corr-existing");

    paymentProducer.publishPaymentInitiated(initiatedEvent);

    verify(kafkaTemplate).send(messageCaptor.capture());
    assertEquals("corr-existing", initiatedEvent.getCorrelationId());
    assertEquals("corr-existing", messageCaptor.getValue().getHeaders().get("correlation_id"));
  }

  @Test
  void testPublishPaymentInitiatedSendFailureDoesNotPropagate() {
    stubSendFailure();

    assertDoesNotThrow(() -> paymentProducer.publishPaymentInitiated(initiatedEvent));
  }

  @Test
  void testPublishPaymentChargedSendsToChargedTopic() {
    stubSendSuccess();

    paymentProducer.publishPaymentCharged(chargedEvent);

    verify(kafkaTemplate).send(messageCaptor.capture());
    Message<?> message = messageCaptor.getValue();
    assertEquals(KafkaTopics.PAYMENT_CHARGED, message.getHeaders().get(KafkaHeaders.TOPIC));
    assertEquals("pay_123", message.getHeaders().get(KafkaHeaders.RECEIVED_KEY));
    assertEquals(chargedEvent, message.getPayload());
  }

  @Test
  void testPublishPaymentChargedSendFailureDoesNotPropagate() {
    stubSendFailure();

    assertDoesNotThrow(() -> paymentProducer.publishPaymentCharged(chargedEvent));
  }

  @Test
  void testPublishPaymentCompletedSendsToCompletedTopic() {
    stubSendSuccess();

    paymentProducer.publishPaymentCompleted(completedEvent);

    verify(kafkaTemplate).send(messageCaptor.capture());
    Message<?> message = messageCaptor.getValue();
    assertEquals(KafkaTopics.PAYMENT_COMPLETED, message.getHeaders().get(KafkaHeaders.TOPIC));
    assertEquals("pay_123", message.getHeaders().get(KafkaHeaders.RECEIVED_KEY));
    assertEquals(completedEvent, message.getPayload());
  }

  @Test
  void testPublishPaymentCompletedSendFailureDoesNotPropagate() {
    stubSendFailure();

    assertDoesNotThrow(() -> paymentProducer.publishPaymentCompleted(completedEvent));
  }
}

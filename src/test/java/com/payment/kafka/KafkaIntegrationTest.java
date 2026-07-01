package com.payment.kafka;

import com.payment.events.PaymentInitiatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Kafka payment event processing.
 *
 * <p>Verifies that payment events are correctly serialized, published, and consumed.
 * Uses embedded Kafka broker for isolated testing.
 *
 * @author orvigas@gmail.com
 */
@SpringBootTest
@EmbeddedKafka(partitions = 1, topics = KafkaTopics.PAYMENT_INITIATED)
@ActiveProfiles("test")
@Slf4j
public class KafkaIntegrationTest {

  @Autowired
  private KafkaTemplate<String, Object> kafkaTemplate;

  @Autowired
  private EmbeddedKafkaBroker embeddedKafka;

  @Autowired
  private ConsumerFactory<String, PaymentInitiatedEvent> paymentInitiatedEventConsumerFactory;

  private Consumer<String, PaymentInitiatedEvent> consumer;

  @BeforeEach
  void setUp() {
    consumer = paymentInitiatedEventConsumerFactory.createConsumer();
    consumer.subscribe(Collections.singletonList(KafkaTopics.PAYMENT_INITIATED));
  }

  @AfterEach
  void tearDown() {
    consumer.close();
  }

  /**
   * Tests that payment initiated events are correctly published and consumed.
   *
   * <p>Verifies:
   * <ul>
   * <li>Event is published to Kafka
   * <li>Event is consumed from the topic
   * <li>Event key and value are correctly deserialized
   * </ul>
   */
  @Test
  void testPublishPaymentInitiatedEvent() {
    // Arrange
    PaymentInitiatedEvent event = PaymentInitiatedEvent.builder()
        .paymentId("pay_123")
        .userId("user_456")
        .amount(new BigDecimal("5000.00"))
        .currency("MXN")
        .merchant("jersey-mikes")
        .createdAt(LocalDateTime.now())
        .build();

    // Act
    kafkaTemplate.send(KafkaTopics.PAYMENT_INITIATED, event.getPaymentId(), event);

    // Assert
    ConsumerRecord<String, PaymentInitiatedEvent> record = KafkaTestUtils.getSingleRecord(consumer,
        KafkaTopics.PAYMENT_INITIATED, Duration.ofSeconds(5));
    assertNotNull(record);
    assertEquals(event.getPaymentId(), record.key());

    PaymentInitiatedEvent consumedEvent = record.value();
    assertNotNull(consumedEvent);
    assertEquals(event.getPaymentId(), consumedEvent.getPaymentId());
    assertEquals(event.getUserId(), consumedEvent.getUserId());
    assertEquals(event.getAmount(), consumedEvent.getAmount());
    assertEquals(event.getCurrency(), consumedEvent.getCurrency());
    assertEquals(event.getMerchant(), consumedEvent.getMerchant());

    log.info("Test passed: Event published and consumed successfully");
  }
}
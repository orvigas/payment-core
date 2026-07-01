package com.payment.kafka;

import java.util.UUID;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import com.payment.events.PaymentChargedEvent;
import com.payment.events.PaymentCompletedEvent;
import com.payment.events.PaymentInitiatedEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Producer for publishing payment events to Kafka.
 *
 * <p>Responsible for sending payment-related events to Kafka topics for consumption by
 * downstream services. Each published message includes correlation IDs for distributed tracing.
 *
 * @author orvigas@gmail.com
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentProducer {

  private final KafkaTemplate<String, Object> kafkaTemplate;

  /**
   * Publishes a payment initiated event to the payment-initiated topic.
   *
   * <p>Generates a correlation ID if not present for request tracing across services.
   *
   * @param event the payment initiation event to publish
   */
  public void publishPaymentInitiated(PaymentInitiatedEvent event) {
    log.info("Publishing PaymentInitiatedEvent for payment: {}", event.getPaymentId());

    // Add correlation ID for tracing
    if (event.getCorrelationId() == null) {
      event.setCorrelationId(UUID.randomUUID().toString());
    }

    // Create message with headers
    Message<PaymentInitiatedEvent> message = MessageBuilder
        .withPayload(event)
        .setHeader(KafkaHeaders.TOPIC, KafkaTopics.PAYMENT_INITIATED)
        .setHeader(KafkaHeaders.RECEIVED_KEY, event.getPaymentId())
        .setHeader("correlation_id", event.getCorrelationId())
        .build();

    // Send
    kafkaTemplate.send(message)
        .whenComplete((result, ex) -> {
          if (ex == null) {
            log.debug("PaymentInitiatedEvent published successfully for payment: {}", event.getPaymentId());
          } else {
            log.error("Error publishing PaymentInitiatedEvent for payment: {}", event.getPaymentId(), ex);
          }
        });
  }

  /**
   * Publishes a payment charged event to the payment-charged topic.
   *
   * @param event the payment charged event to publish
   */
  public void publishPaymentCharged(PaymentChargedEvent event) {
    log.info("Publishing PaymentChargedEvent for payment: {}", event.getPaymentId());

    Message<PaymentChargedEvent> message = MessageBuilder
        .withPayload(event)
        .setHeader(KafkaHeaders.TOPIC, KafkaTopics.PAYMENT_CHARGED)
        .setHeader(KafkaHeaders.RECEIVED_KEY, event.getPaymentId())
        .build();

    kafkaTemplate.send(message)
        .whenComplete((result, ex) -> {
          if (ex == null) {
            log.debug("PaymentChargedEvent published for payment: {}", event.getPaymentId());
          } else {
            log.error("Error publishing PaymentChargedEvent for payment: {}", event.getPaymentId(), ex);
          }
        });
  }

  /**
   * Publishes a payment completed event to the payment-completed topic.
   *
   * @param event the payment completion event to publish
   */
  public void publishPaymentCompleted(PaymentCompletedEvent event) {
    log.info("Publishing PaymentCompletedEvent for payment: {}", event.getPaymentId());

    Message<PaymentCompletedEvent> message = MessageBuilder
        .withPayload(event)
        .setHeader(KafkaHeaders.TOPIC, KafkaTopics.PAYMENT_COMPLETED)
        .setHeader(KafkaHeaders.RECEIVED_KEY, event.getPaymentId())
        .build();

    kafkaTemplate.send(message)
        .whenComplete((result, ex) -> {
          if (ex == null) {
            log.debug("PaymentCompletedEvent published for payment: {}", event.getPaymentId());
          } else {
            log.error("Error publishing PaymentCompletedEvent for payment: {}", event.getPaymentId(), ex);
          }
        });
  }
}
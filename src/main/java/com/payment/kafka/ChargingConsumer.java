package com.payment.kafka;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.payment.events.PaymentChargedEvent;
import com.payment.events.PaymentInitiatedEvent;
import com.payment.models.Payment;
import com.payment.models.PaymentStatus;
import com.payment.repositories.PaymentRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Consumer for processing payment charging events.
 *
 * <p>
 * Subscribes to {@link com.payment.kafka.KafkaTopics#PAYMENT_INITIATED} events
 * and orchestrates
 * the payment charging process. Updates payment status to PROCESSING, simulates
 * the charge operation,
 * and publishes the result to the payment-charged topic.
 *
 * @author orvigas@gmail.com
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ChargingConsumer {

  private final PaymentRepository paymentRepository;
  private final PaymentProducer paymentProducer;

  /**
   * Consumes payment initiated events and processes the charge.
   *
   * <p>
   * Retrieves the payment from the database, updates status to PROCESSING,
   * simulates
   * the charge operation (90% success rate), and updates the final status to
   * COMPLETED
   * or FAILED. Publishes a payment charged event with the result.
   *
   * @param event         the payment initiation event containing payment details
   * @param correlationId optional correlation ID for request tracing
   * @throws RuntimeException if payment is not found or an error occurs during
   *                          processing
   */
  @KafkaListener(topics = KafkaTopics.PAYMENT_INITIATED, groupId = KafkaTopics.CHARGING_GROUP, containerFactory = "kafkaListenerContainerFactory")
  @Transactional
  public void consumePaymentInitiated(
      @Payload PaymentInitiatedEvent event,
      @Header(name = "correlation_id", required = false) String correlationId) {

    log.info("ChargingConsumer: Processing PaymentInitiatedEvent for payment: {}", event.getPaymentId());

    try {
      // Fetch payment from DB
      Payment payment = paymentRepository.findByPaymentId(event.getPaymentId())
          .orElseThrow(() -> new RuntimeException("Payment not found: " + event.getPaymentId()));

      // Update status to PROCESSING
      payment.setStatus(PaymentStatus.PROCESSING);
      paymentRepository.save(payment);
      log.debug("Payment marked as PROCESSING: {}", event.getPaymentId());

      // Simulate external processor call
      boolean chargeSuccessful = processCharge(event);

      // Update payment status based on charge result
      payment.setStatus(chargeSuccessful ? PaymentStatus.COMPLETED : PaymentStatus.FAILED);
      paymentRepository.save(payment);
      log.debug("Payment status updated to {}: {}", payment.getStatus(), event.getPaymentId());

      // Publish result event
      PaymentChargedEvent chargedEvent = PaymentChargedEvent.builder()
          .paymentId(event.getPaymentId())
          .transactionId(UUID.randomUUID().toString())
          .chargedAt(LocalDateTime.now())
          .success(chargeSuccessful)
          .correlationId(event.getCorrelationId())
          .errorMessage(chargeSuccessful ? null : "Simulated charge failure")
          .build();

      paymentProducer.publishPaymentCharged(chargedEvent);

      log.info("ChargingConsumer: Completed for payment: {}", event.getPaymentId());

    } catch (Exception e) {
      log.error("ChargingConsumer: Error processing payment: {}", event.getPaymentId(), e);
      // In production: publish PaymentFailedEvent
      throw new RuntimeException(e);
    }
  }

  /**
   * Simulates the payment charge operation.
   *
   * <p>
   * In a production system, this would call an external payment processor
   * (e.g., Stripe, Adyen). Currently returns success with 90% probability.
   *
   * @param event the payment initiation event
   * @return true if charge succeeds, false if charge fails
   */
  private boolean processCharge(PaymentInitiatedEvent event) {
    // Simulate charge processing
    // In real scenario: call Stripe, Adyen, etc.

    // 90% success rate
    boolean success = Math.random() < 0.9;

    if (success) {
      log.debug("Charge successful for payment: {}", event.getPaymentId());
    } else {
      log.warn("Charge failed for payment: {}", event.getPaymentId());
    }

    return success;
  }
}
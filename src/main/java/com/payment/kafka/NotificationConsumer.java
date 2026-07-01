package com.payment.kafka;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import com.payment.events.PaymentChargedEvent;

import lombok.extern.slf4j.Slf4j;

/**
 * Consumer for payment notification events.
 *
 * <p>Subscribes to {@link com.payment.kafka.KafkaTopics#PAYMENT_CHARGED} events and sends
 * notifications to users based on payment charge results.
 *
 * @author orvigas@gmail.com
 */
@Component
@Slf4j
public class NotificationConsumer {

  /**
   * Processes payment charged events and sends notifications.
   *
   * <p>Sends a success notification if the charge succeeded, or a failure notification
   * with retry instructions if the charge failed.
   *
   * @param event the payment charged event containing charge result
   * @param correlationId optional correlation ID for request tracing
   */
  @KafkaListener(topics = KafkaTopics.PAYMENT_CHARGED, groupId = KafkaTopics.NOTIFICATION_GROUP, containerFactory = "kafkaListenerContainerFactory")
  public void consumePaymentCharged(
      @Payload PaymentChargedEvent event,
      @Header(name = "correlation_id", required = false) String correlationId) {

    log.info("NotificationConsumer: Processing PaymentChargedEvent for payment: {}", event.getPaymentId());

    try {
      if (event.isSuccess()) {
        sendSuccessNotification(event);
      } else {
        sendFailureNotification(event);
      }

      log.info("NotificationConsumer: Completed for payment: {}", event.getPaymentId());

    } catch (Exception e) {
      log.error("NotificationConsumer: Error processing event for payment: {}", event.getPaymentId(), e);
      throw new RuntimeException(e);
    }
  }

  /**
   * Sends a success notification to the customer.
   *
   * <p>In production, this would send an email via a service like SendGrid or Twilio.
   *
   * @param event the payment charged event
   */
  private void sendSuccessNotification(PaymentChargedEvent event) {
    // In real scenario: send email via SendGrid, Twilio, etc.
    log.debug("Notification: Sending success email for payment: {}", event.getPaymentId());
    log.info("Email sent: Payment {} charged successfully", event.getPaymentId());
  }

  /**
   * Sends a failure notification to the customer.
   *
   * <p>In production, this would send an email with retry instructions and support contact.
   *
   * @param event the payment charged event containing failure details
   */
  private void sendFailureNotification(PaymentChargedEvent event) {
    // In real scenario: send email with retry instructions
    log.debug("Notification: Sending failure email for payment: {}", event.getPaymentId());
    log.warn("Email sent: Payment {} failed. Reason: {}", event.getPaymentId(), event.getErrorMessage());
  }
}

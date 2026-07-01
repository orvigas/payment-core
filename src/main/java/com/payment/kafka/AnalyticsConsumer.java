package com.payment.kafka;

import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import com.payment.events.PaymentChargedEvent;

import lombok.extern.slf4j.Slf4j;

/**
 * Consumer for payment analytics events.
 *
 * <p>Subscribes to {@link com.payment.kafka.KafkaTopics#PAYMENT_CHARGED} events and tracks
 * success and failure metrics for payment charges.
 *
 * @author orvigas@gmail.com
 */
@Component
@Slf4j
public class AnalyticsConsumer {

  private final AtomicInteger successfulCharges = new AtomicInteger(0);
  private final AtomicInteger failedCharges = new AtomicInteger(0);

  /**
   * Processes payment charged events and updates analytics metrics.
   *
   * @param event the payment charged event containing charge result
   */
  @KafkaListener(topics = KafkaTopics.PAYMENT_CHARGED, groupId = KafkaTopics.ANALYTICS_GROUP, containerFactory = "kafkaListenerContainerFactory")
  public void consumePaymentCharged(@Payload PaymentChargedEvent event) {
    log.debug("AnalyticsConsumer: Logging event for payment: {}", event.getPaymentId());

    if (event.isSuccess()) {
      int total = successfulCharges.incrementAndGet();
      log.info("Analytics: Successful charges: {}", total);
    } else {
      int total = failedCharges.incrementAndGet();
      log.warn("Analytics: Failed charges: {}", total);
    }
  }

  /**
   * Returns the count of successful charges processed.
   *
   * @return number of successful charges
   */
  public int getSuccessfulCharges() {
    return successfulCharges.get();
  }

  /**
   * Returns the count of failed charges processed.
   *
   * @return number of failed charges
   */
  public int getFailedCharges() {
    return failedCharges.get();
  }
}
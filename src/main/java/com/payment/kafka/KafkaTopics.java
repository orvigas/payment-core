package com.payment.kafka;

/**
 * Central repository of Kafka topic and consumer group names.
 *
 * <p>Defines the topics used for payment event streaming and the consumer groups that process
 * events from those topics. All services must use these constants to ensure consistent topic
 * naming and subscriptions.
 *
 * @author orvigas@gmail.com
 */
public class KafkaTopics {

  /** Topic for payment initiation events. */
  public static final String PAYMENT_INITIATED = "payment-initiated";

  /** Topic for payment charging events. */
  public static final String PAYMENT_CHARGED = "payment-charged";

  /** Topic for payment completion events. */
  public static final String PAYMENT_COMPLETED = "payment-completed";

  /** Topic for payment failure events. */
  public static final String PAYMENT_FAILED = "payment-failed";

  /** Consumer group for charging service. */
  public static final String CHARGING_GROUP = "charging-service";

  /** Consumer group for notification service. */
  public static final String NOTIFICATION_GROUP = "notification-service";

  /** Consumer group for analytics service. */
  public static final String ANALYTICS_GROUP = "analytics-service";

  /** Private constructor to prevent instantiation. */
  private KafkaTopics() {
  }
}
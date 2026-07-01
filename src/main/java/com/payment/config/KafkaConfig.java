package com.payment.config;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import com.payment.kafka.KafkaTopics;
import com.payment.events.PaymentInitiatedEvent;

import lombok.extern.slf4j.Slf4j;

/**
 * Kafka configuration for payment event streaming.
 *
 * <p>
 * Configures Kafka topics, producer factories, and consumer factories for the
 * payment system.
 * Enables asynchronous event-driven communication across payment services.
 *
 * <p>
 * Topics configured:
 * <ul>
 * <li>{@link com.payment.kafka.KafkaTopics#PAYMENT_INITIATED} - Payment
 * creation events
 * <li>{@link com.payment.kafka.KafkaTopics#PAYMENT_CHARGED} - Payment
 * processing events
 * <li>{@link com.payment.kafka.KafkaTopics#PAYMENT_COMPLETED} - Completion
 * events
 * <li>{@link com.payment.kafka.KafkaTopics#PAYMENT_FAILED} - Failure events
 * </ul>
 *
 * @author orvigas@gmail.com
 */
@Configuration
@EnableKafka
@Slf4j
public class KafkaConfig {

  @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
  private String bootstrapServers;

  // ==================== Admin ====================

  @Bean
  public org.springframework.kafka.core.KafkaAdmin kafkaAdmin() {
    Map<String, Object> configs = new HashMap<>();
    configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    return new org.springframework.kafka.core.KafkaAdmin(configs);
  }

  // ==================== Topics ====================

  @Bean
  public NewTopic paymentInitiatedTopic() {
    return TopicBuilder.name(KafkaTopics.PAYMENT_INITIATED)
        .partitions(3)
        .replicas(1)
        .build();
  }

  @Bean
  public NewTopic paymentChargedTopic() {
    return TopicBuilder.name(KafkaTopics.PAYMENT_CHARGED)
        .partitions(3)
        .replicas(1)
        .build();
  }

  @Bean
  public NewTopic paymentCompletedTopic() {
    return TopicBuilder.name(KafkaTopics.PAYMENT_COMPLETED)
        .partitions(3)
        .replicas(1)
        .build();
  }

  @Bean
  public NewTopic paymentFailedTopic() {
    return TopicBuilder.name(KafkaTopics.PAYMENT_FAILED)
        .partitions(3)
        .replicas(1)
        .build();
  }

  // ==================== Producer ====================

  @Bean
  public ProducerFactory<String, Object> producerFactory() {
    Map<String, Object> configProps = new HashMap<>();
    configProps.put(org.apache.kafka.clients.producer.ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    configProps.put(org.apache.kafka.clients.producer.ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
        StringSerializer.class);
    configProps.put(org.apache.kafka.clients.producer.ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
        JsonSerializer.class);
    configProps.put(org.apache.kafka.clients.producer.ProducerConfig.ACKS_CONFIG, "all");
    configProps.put(org.apache.kafka.clients.producer.ProducerConfig.RETRIES_CONFIG, 3);
    configProps.put(org.apache.kafka.clients.producer.ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy");

    return new DefaultKafkaProducerFactory<>(configProps);
  }

  @Bean
  public KafkaTemplate<String, Object> kafkaTemplate() {
    return new KafkaTemplate<>(producerFactory());
  }

  // ==================== Consumer ====================

  @Bean
  public ConsumerFactory<String, Object> consumerFactory() {
    Map<String, Object> configProps = new HashMap<>();
    configProps.put(org.apache.kafka.clients.consumer.ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    configProps.put(org.apache.kafka.clients.consumer.ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
        StringDeserializer.class);
    configProps.put(org.apache.kafka.clients.consumer.ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
        JsonDeserializer.class);
    configProps.put(org.apache.kafka.clients.consumer.ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
    configProps.put(JsonDeserializer.VALUE_DEFAULT_TYPE, "java.lang.Object");
    configProps.put(JsonDeserializer.TRUSTED_PACKAGES, "com.payment.events");

    return new DefaultKafkaConsumerFactory<>(configProps);
  }

  @Bean
  public KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<String, Object>> kafkaListenerContainerFactory() {
    ConcurrentKafkaListenerContainerFactory<String, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
    factory.setConsumerFactory(consumerFactory());
    factory.setConcurrency(3); // 3 parallel consumers
    return factory;
  }

  // ==================== Typed Consumer Factories ====================

  /**
   * Creates a typed consumer factory for PaymentInitiatedEvent.
   *
   * <p>Used primarily for testing and specialized consumers that require type-safe deserialization.
   *
   * @return consumer factory configured for PaymentInitiatedEvent deserialization
   */
  @Bean
  public ConsumerFactory<String, PaymentInitiatedEvent> paymentInitiatedEventConsumerFactory() {
    Map<String, Object> configProps = new HashMap<>();
    configProps.put(org.apache.kafka.clients.consumer.ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    configProps.put(org.apache.kafka.clients.consumer.ConsumerConfig.GROUP_ID_CONFIG, "test-payment-initiated-event-consumer");
    configProps.put(org.apache.kafka.clients.consumer.ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
        StringDeserializer.class);
    configProps.put(org.apache.kafka.clients.consumer.ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
        JsonDeserializer.class);
    configProps.put(org.apache.kafka.clients.consumer.ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
    configProps.put(JsonDeserializer.VALUE_DEFAULT_TYPE, PaymentInitiatedEvent.class.getName());
    configProps.put(JsonDeserializer.TRUSTED_PACKAGES, "com.payment.events");

    return new DefaultKafkaConsumerFactory<>(configProps);
  }
}
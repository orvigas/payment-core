package com.payment.config;

import java.util.HashMap;
import java.util.Map;

import io.micrometer.observation.ObservationRegistry;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
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
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.lang.NonNull;

import com.payment.events.PaymentInitiatedEvent;
import com.payment.kafka.KafkaTopics;

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
public class KafkaConfig {

  @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
  private String bootstrapServers;

  /**
   * Creates the admin client used to provision the payment topics at startup.
   *
   * @return configured Kafka admin client
   */
  @Bean
  KafkaAdmin kafkaAdmin() {
    Map<String, Object> configs = new HashMap<>();
    configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    return new KafkaAdmin(configs);
  }

  /**
   * Declares the payment-initiated topic.
   *
   * @return topic definition
   */
  @Bean
  NewTopic paymentInitiatedTopic() {
    return TopicBuilder.name(KafkaTopics.PAYMENT_INITIATED)
        .partitions(3)
        .replicas(1)
        .build();
  }

  /**
   * Declares the payment-charged topic.
   *
   * @return topic definition
   */
  @Bean
  NewTopic paymentChargedTopic() {
    return TopicBuilder.name(KafkaTopics.PAYMENT_CHARGED)
        .partitions(3)
        .replicas(1)
        .build();
  }

  /**
   * Declares the payment-completed topic.
   *
   * @return topic definition
   */
  @Bean
  NewTopic paymentCompletedTopic() {
    return TopicBuilder.name(KafkaTopics.PAYMENT_COMPLETED)
        .partitions(3)
        .replicas(1)
        .build();
  }

  /**
   * Declares the payment-failed topic.
   *
   * @return topic definition
   */
  @Bean
  NewTopic paymentFailedTopic() {
    return TopicBuilder.name(KafkaTopics.PAYMENT_FAILED)
        .partitions(3)
        .replicas(1)
        .build();
  }

  /**
   * Creates the producer factory used to publish payment events as JSON.
   *
   * @return producer factory for payment events
   */
  @Bean
  ProducerFactory<String, Object> producerFactory() {
    Map<String, Object> configProps = new HashMap<>();
    configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
    configProps.put(ProducerConfig.ACKS_CONFIG, "all");
    configProps.put(ProducerConfig.RETRIES_CONFIG, 3);
    configProps.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy");

    return new DefaultKafkaProducerFactory<>(configProps);
  }

  /**
   * Creates the template used by producers to send payment events.
   *
   * <p>Observation is enabled explicitly because this template is built manually rather
   * than through Spring Boot's Kafka autoconfiguration, which is what normally wires
   * tracing in for free - without this, publishing a message creates no span at all, and
   * the trace never crosses the Kafka hop into the consumers below.
   *
   * @param producerFactory factory supplying producer instances
   * @param observationRegistry registry that spans and Kafka metrics are recorded through
   * @return Kafka template for publishing payment events
   */
  @Bean
  KafkaTemplate<String, Object> kafkaTemplate(
      @NonNull ProducerFactory<String, Object> producerFactory, @NonNull ObservationRegistry observationRegistry) {
    KafkaTemplate<String, Object> template = new KafkaTemplate<>(producerFactory);
    template.setObservationEnabled(true);
    template.setObservationRegistry(observationRegistry);
    return template;
  }

  /**
   * Creates the generic consumer factory for payment event listeners.
   *
   * @return consumer factory with JSON deserialization
   */
  @Bean
  ConsumerFactory<String, Object> consumerFactory() {
    Map<String, Object> configProps = new HashMap<>();
    configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
    configProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
    configProps.put(JsonDeserializer.VALUE_DEFAULT_TYPE, "java.lang.Object");
    configProps.put(JsonDeserializer.TRUSTED_PACKAGES, "com.payment.events");

    return new DefaultKafkaConsumerFactory<>(configProps);
  }

  /**
   * Creates the listener container factory for {@code @KafkaListener} methods.
   *
   * <p>Concurrency matches the topic partition count so each partition gets a consumer.
   * Observation is enabled explicitly for the same reason as {@link #kafkaTemplate} -
   * this factory is built manually, not through Spring Boot's Kafka autoconfiguration,
   * so nothing turns tracing on for the consumer side by default.
   *
   * @param consumerFactory factory supplying consumer instances
   * @param observationRegistry registry that spans and Kafka metrics are recorded through
   * @return listener container factory
   */
  @Bean
  KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<String, Object>> kafkaListenerContainerFactory(
      @NonNull ConsumerFactory<String, Object> consumerFactory, @NonNull ObservationRegistry observationRegistry) {
    ConcurrentKafkaListenerContainerFactory<String, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
    factory.setConsumerFactory(consumerFactory);
    factory.setConcurrency(3);
    factory.getContainerProperties().setObservationEnabled(true);
    factory.getContainerProperties().setObservationRegistry(observationRegistry);
    return factory;
  }

  /**
   * Creates a typed consumer factory for PaymentInitiatedEvent.
   *
   * <p>Used primarily for testing and specialized consumers that require type-safe deserialization.
   *
   * @return consumer factory configured for PaymentInitiatedEvent deserialization
   */
  @Bean
  ConsumerFactory<String, PaymentInitiatedEvent> paymentInitiatedEventConsumerFactory() {
    Map<String, Object> configProps = new HashMap<>();
    configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    configProps.put(ConsumerConfig.GROUP_ID_CONFIG, "test-payment-initiated-event-consumer");
    configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
    configProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
    configProps.put(JsonDeserializer.VALUE_DEFAULT_TYPE, PaymentInitiatedEvent.class.getName());
    configProps.put(JsonDeserializer.TRUSTED_PACKAGES, "com.payment.events");

    return new DefaultKafkaConsumerFactory<>(configProps);
  }
}

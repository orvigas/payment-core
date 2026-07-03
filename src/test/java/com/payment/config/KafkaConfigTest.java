package com.payment.config;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.test.context.ActiveProfiles;

import com.payment.events.PaymentInitiatedEvent;

/**
 * Tests for KafkaConfig.
 *
 * @author orvigas@gmail.com
 */
@SpringBootTest
@ActiveProfiles("test")
class KafkaConfigTest {

  @Autowired
  private ProducerFactory<String, Object> producerFactory;

  @Autowired
  private ConsumerFactory<String, PaymentInitiatedEvent> paymentInitiatedConsumerFactory;

  @Test
  void testProducerFactoryConfiguration() {
    assertNotNull(producerFactory);
  }

  @Test
  void testConsumerFactoryConfiguration() {
    assertNotNull(paymentInitiatedConsumerFactory);
  }

  @Test
  void testKafkaConfigExists() {
    assertNotNull(producerFactory);
  }

  @Test
  void testKafkaBeansAreConfigured() {
    assertNotNull(producerFactory);
    assertNotNull(paymentInitiatedConsumerFactory);
  }
}

package com.payment.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.micrometer.core.instrument.MeterRegistry;

/**
 * Tests for ResilienceConfig.
 *
 * @author orvigas@gmail.com
 */
@SpringBootTest
class ResilienceConfigTest {

  @Autowired
  private CircuitBreaker chargerCircuitBreaker;

  @Autowired
  private CircuitBreaker notificationCircuitBreaker;

  @Autowired
  private Retry chargerRetry;

  @Autowired
  private Retry kafkaProducerRetry;

  @Autowired
  private TimeLimiter chargerTimeLimiter;

  @Autowired
  private TimeLimiter databaseTimeLimiter;

  @Autowired
  private MeterRegistry meterRegistry;

  @Test
  void testChargerCircuitBreakerConfiguration() {
    assertNotNull(chargerCircuitBreaker);
    assertEquals("charger-service", chargerCircuitBreaker.getName());
  }

  @Test
  void testNotificationCircuitBreakerConfiguration() {
    assertNotNull(notificationCircuitBreaker);
    assertEquals("notification-service", notificationCircuitBreaker.getName());
  }

  @Test
  void testChargerRetryConfiguration() {
    assertNotNull(chargerRetry);
    assertEquals("charger-retry", chargerRetry.getName());
  }

  @Test
  void testKafkaProducerRetryConfiguration() {
    assertNotNull(kafkaProducerRetry);
    assertEquals("kafka-producer-retry", kafkaProducerRetry.getName());
  }

  @Test
  void testChargerTimeLimiterConfiguration() {
    assertNotNull(chargerTimeLimiter);
    assertEquals("charger-timeout", chargerTimeLimiter.getName());
  }

  @Test
  void testDatabaseTimeLimiterConfiguration() {
    assertNotNull(databaseTimeLimiter);
    assertEquals("database-timeout", databaseTimeLimiter.getName());
  }

  @Test
  void testMeterRegistryConfiguration() {
    assertNotNull(meterRegistry);
  }

  @Test
  void testAllResiliencBeansAreConfigured() {
    assertNotNull(chargerCircuitBreaker);
    assertNotNull(notificationCircuitBreaker);
    assertNotNull(chargerRetry);
    assertNotNull(kafkaProducerRetry);
    assertNotNull(chargerTimeLimiter);
    assertNotNull(databaseTimeLimiter);
    assertNotNull(meterRegistry);
  }

  // The tests below build standalone instances instead of mutating the shared
  // Spring beans, so circuit breaker state changes cannot leak into other tests.

  @Test
  void testCircuitBreakerRegistryTracksEntryLifecycle() {
    ResilienceConfig config = new ResilienceConfig();
    CircuitBreakerRegistry registry = config.circuitBreakerRegistry();
    config.chargerCircuitBreaker(registry);

    assertTrue(registry.find("charger-service").isPresent());

    registry.remove("charger-service");
    assertTrue(registry.find("charger-service").isEmpty());
  }

  @Test
  void testChargerCircuitBreakerEventListeners() {
    ResilienceConfig config = new ResilienceConfig();
    CircuitBreaker cb = config.chargerCircuitBreaker(config.circuitBreakerRegistry());

    cb.onSuccess(10, TimeUnit.MILLISECONDS);
    cb.onError(10, TimeUnit.MILLISECONDS, new RuntimeException("processor unavailable"));
    cb.transitionToOpenState();

    assertEquals(CircuitBreaker.State.OPEN, cb.getState());
  }

  @Test
  void testChargerRetryDoesNotRetryIllegalArgument() {
    ResilienceConfig config = new ResilienceConfig();
    Retry retry = config.chargerRetry(RetryRegistry.ofDefaults());

    AtomicInteger attempts = new AtomicInteger();
    Supplier<String> supplier = Retry.decorateSupplier(retry, () -> {
      attempts.incrementAndGet();
      throw new IllegalArgumentException("invalid payment data");
    });

    assertThrows(IllegalArgumentException.class, supplier::get);
    assertEquals(1, attempts.get());
  }

  @Test
  void testChargerRetryRetriesTransientFailures() {
    ResilienceConfig config = new ResilienceConfig();
    Retry retry = config.chargerRetry(RetryRegistry.ofDefaults());

    AtomicInteger attempts = new AtomicInteger();
    Supplier<String> supplier = Retry.decorateSupplier(retry, () -> {
      if (attempts.incrementAndGet() < 3) {
        throw new RuntimeException("transient failure");
      }
      return "charged";
    });

    assertEquals("charged", supplier.get());
    assertEquals(3, attempts.get());
  }
}

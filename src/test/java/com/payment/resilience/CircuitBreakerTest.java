package com.payment.resilience;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import lombok.extern.slf4j.Slf4j;

/**
 * Tests for Resilience4j circuit breaker behavior.
 *
 * @author orvigas@gmail.com
 */
@Slf4j
public class CircuitBreakerTest {

  private CircuitBreaker circuitBreaker;

  @BeforeEach
  void setUp() {
    CircuitBreakerConfig config = CircuitBreakerConfig.custom()
        .failureRateThreshold(50.0f)
        .waitDurationInOpenState(java.time.Duration.ofMillis(100))
        .permittedNumberOfCallsInHalfOpenState(3)
        .slidingWindowSize(3)
        .minimumNumberOfCalls(2)
        .build();

    circuitBreaker = CircuitBreaker.of("test-breaker", config);
  }

  @Test
  void testCircuitBreakerOpenAfterFailures() {
    log.info("Testing circuit breaker opens after multiple failures");

    for (int i = 0; i < 3; i++) {
      assertThrows(RuntimeException.class, () -> circuitBreaker.executeSupplier(() -> {
        throw new RuntimeException("Service down");
      }));
    }

    assertThrows(io.github.resilience4j.circuitbreaker.CallNotPermittedException.class, () -> circuitBreaker.executeSupplier(() -> {
      throw new RuntimeException("Service down");
    }));

    log.info("Circuit breaker correctly opened after failures");
  }

  @Test
  void testCircuitBreakerFailFast() {
    log.info("Testing circuit breaker fails fast when open");

    for (int i = 0; i < 3; i++) {
      assertThrows(RuntimeException.class, () -> circuitBreaker.executeSupplier(() -> {
        throw new RuntimeException("Service down");
      }));
    }

    long startTime = System.currentTimeMillis();
    assertThrows(io.github.resilience4j.circuitbreaker.CallNotPermittedException.class, () -> circuitBreaker.executeSupplier(() -> {
      try {
        Thread.sleep(5000);
      } catch (InterruptedException e) {
        log.error(e.getMessage());
      }
      return "success";
    }));
    long duration = System.currentTimeMillis() - startTime;

    assertTrue(duration < 100, "Circuit breaker should fail fast, but took " + duration + "ms");
    log.info("Circuit breaker failed fast in {}ms", duration);
  }
}
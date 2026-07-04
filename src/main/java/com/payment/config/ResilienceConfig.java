package com.payment.config;

import java.time.Duration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import io.github.resilience4j.micrometer.tagged.TaggedCircuitBreakerMetrics;
import io.github.resilience4j.micrometer.tagged.TaggedRetryMetrics;
import io.github.resilience4j.micrometer.tagged.TaggedTimeLimiterMetrics;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import lombok.extern.slf4j.Slf4j;

/**
 * Resilience4j and metrics configuration.
 *
 * <p>Defines the circuit breakers, retries, and time limiters that guard calls
 * to the external charger, notification delivery, Kafka publishing, and the
 * database, plus the Prometheus meter registry used for metrics export.
 *
 * @author orvigas@gmail.com
 */
@Configuration
@Slf4j
public class ResilienceConfig {

  /**
   * Registry for all circuit breakers, with lifecycle logging.
   *
   * @return the circuit breaker registry
   */
  @Bean
  public CircuitBreakerRegistry circuitBreakerRegistry() {
    CircuitBreakerRegistry registry = CircuitBreakerRegistry.ofDefaults();
    registry.getEventPublisher()
        .onEntryAdded(event -> log.debug("Circuit breaker added: {}", event.getAddedEntry().getName()))
        .onEntryRemoved(event -> log.debug("Circuit breaker removed: {}", event.getRemovedEntry().getName()));
    return registry;
  }

  /**
   * Circuit breaker guarding calls to the external charger.
   *
   * <p>Opens at 50% failures or 50% slow calls (over 2s), then waits 30s before
   * probing with 3 half-open calls.
   *
   * @param registry the circuit breaker registry
   * @return the charger circuit breaker
   */
  @Bean
  public CircuitBreaker chargerCircuitBreaker(CircuitBreakerRegistry registry) {
    CircuitBreakerConfig config = CircuitBreakerConfig.custom()
        .failureRateThreshold(50.0f)
        .slowCallRateThreshold(50.0f)
        .slowCallDurationThreshold(Duration.ofSeconds(2))
        .waitDurationInOpenState(Duration.ofSeconds(30))
        .permittedNumberOfCallsInHalfOpenState(3)
        .automaticTransitionFromOpenToHalfOpenEnabled(true)
        .recordExceptions(Exception.class)
        .ignoreExceptions()
        .build();

    CircuitBreaker cb = registry.circuitBreaker("charger-service", config);
    cb.getEventPublisher()
        .onStateTransition(event -> log.warn("Circuit breaker {} transitioned from {} to {}",
            "charger-service", event.getStateTransition().getFromState(), event.getStateTransition().getToState()))
        .onError(event -> log.error("Circuit breaker charger error: {}", event.getThrowable().getMessage()))
        .onSuccess(event -> log.debug("Circuit breaker charger success"));

    return cb;
  }

  /**
   * Circuit breaker guarding notification delivery.
   *
   * <p>More tolerant than the charger breaker because notifications are not on
   * the critical payment path.
   *
   * @param registry the circuit breaker registry
   * @return the notification circuit breaker
   */
  @Bean
  public CircuitBreaker notificationCircuitBreaker(CircuitBreakerRegistry registry) {
    CircuitBreakerConfig config = CircuitBreakerConfig.custom()
        .failureRateThreshold(60.0f)
        .slowCallRateThreshold(60.0f)
        .slowCallDurationThreshold(Duration.ofSeconds(3))
        .waitDurationInOpenState(Duration.ofSeconds(20))
        .permittedNumberOfCallsInHalfOpenState(2)
        .automaticTransitionFromOpenToHalfOpenEnabled(true)
        .recordExceptions(Exception.class)
        .build();

    return registry.circuitBreaker("notification-service", config);
  }

  /**
   * Registry for all retry instances.
   *
   * @return the retry registry
   */
  @Bean
  public RetryRegistry retryRegistry() {
    return RetryRegistry.ofDefaults();
  }

  /**
   * Retry policy for charger calls.
   *
   * <p>Retries transient failures up to 3 times with exponential backoff, but
   * never retries {@link IllegalArgumentException} since invalid input cannot
   * succeed on retry.
   *
   * @param registry the retry registry
   * @return the charger retry
   */
  @Bean
  public Retry chargerRetry(RetryRegistry registry) {
    RetryConfig config = RetryConfig.custom()
        .maxAttempts(3)
        .intervalFunction(
            io.github.resilience4j.core.IntervalFunction.ofExponentialBackoff(100, 2, 1000)
        )
        .retryOnException(ex -> !(ex instanceof IllegalArgumentException))
        .build();

    return registry.retry("charger-retry", config);
  }

  /**
   * Retry policy for Kafka producer sends.
   *
   * <p>Uses randomized exponential backoff to avoid thundering-herd retries
   * against a recovering broker.
   *
   * @param registry the retry registry
   * @return the Kafka producer retry
   */
  @Bean
  public Retry kafkaProducerRetry(RetryRegistry registry) {
    RetryConfig config = RetryConfig.custom()
        .maxAttempts(5)
        .intervalFunction(
            io.github.resilience4j.core.IntervalFunction.ofExponentialRandomBackoff(50, 2))
        .build();

    return registry.retry("kafka-producer-retry", config);
  }

  /**
   * Registry for all time limiters.
   *
   * @return the time limiter registry
   */
  @Bean
  public TimeLimiterRegistry timeLimiterRegistry() {
    return TimeLimiterRegistry.ofDefaults();
  }

  /**
   * Time limiter for charger calls, failing anything over 5 seconds.
   *
   * @param registry the time limiter registry
   * @return the charger time limiter
   */
  @Bean
  public TimeLimiter chargerTimeLimiter(TimeLimiterRegistry registry) {
    TimeLimiterConfig config = TimeLimiterConfig.custom()
        .timeoutDuration(Duration.ofSeconds(5))
        .cancelRunningFuture(true)
        .build();

    return registry.timeLimiter("charger-timeout", config);
  }

  /**
   * Time limiter for database operations, failing anything over 2 seconds.
   *
   * @param registry the time limiter registry
   * @return the database time limiter
   */
  @Bean
  public TimeLimiter databaseTimeLimiter(TimeLimiterRegistry registry) {
    TimeLimiterConfig config = TimeLimiterConfig.custom()
        .timeoutDuration(Duration.ofSeconds(2))
        .cancelRunningFuture(true)
        .build();

    return registry.timeLimiter("database-timeout", config);
  }

  /**
   * Prometheus-backed meter registry used for the metrics scrape endpoint.
   *
   * @return the meter registry
   */
  @Bean
  public MeterRegistry meterRegistry() {
    return new PrometheusMeterRegistry(
        PrometheusConfig.DEFAULT);
  }

  /**
   * Binds circuit breaker state and call outcome metrics to the meter registry.
   *
   * <p>The registries here are created manually rather than through Resilience4j's Spring
   * Boot autoconfiguration, so metrics have to be bound explicitly too - autoconfiguration
   * only wires metrics for the registry beans it creates itself.
   *
   * @param registry the circuit breaker registry
   * @param meterRegistry the meter registry metrics are exported through
   * @return the bound metrics binder
   */
  @Bean
  public TaggedCircuitBreakerMetrics circuitBreakerMetricsBinder(
      CircuitBreakerRegistry registry, MeterRegistry meterRegistry) {
    TaggedCircuitBreakerMetrics metrics = TaggedCircuitBreakerMetrics.ofCircuitBreakerRegistry(registry);
    metrics.bindTo(meterRegistry);
    return metrics;
  }

  /**
   * Binds retry attempt and outcome metrics to the meter registry.
   *
   * @param registry the retry registry
   * @param meterRegistry the meter registry metrics are exported through
   * @return the bound metrics binder
   */
  @Bean
  public TaggedRetryMetrics retryMetricsBinder(RetryRegistry registry, MeterRegistry meterRegistry) {
    TaggedRetryMetrics metrics = TaggedRetryMetrics.ofRetryRegistry(registry);
    metrics.bindTo(meterRegistry);
    return metrics;
  }

  /**
   * Binds time limiter timeout/success metrics to the meter registry.
   *
   * @param registry the time limiter registry
   * @param meterRegistry the meter registry metrics are exported through
   * @return the bound metrics binder
   */
  @Bean
  public TaggedTimeLimiterMetrics timeLimiterMetricsBinder(
      TimeLimiterRegistry registry, MeterRegistry meterRegistry) {
    TaggedTimeLimiterMetrics metrics = TaggedTimeLimiterMetrics.ofTimeLimiterRegistry(registry);
    metrics.bindTo(meterRegistry);
    return metrics;
  }
}

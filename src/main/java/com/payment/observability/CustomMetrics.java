package com.payment.observability;

import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

/**
 * Custom metrics for payment operations.
 *
 * <p>
 * Tracks payments and charges using counters, timers, and gauges from Micrometer.
 *
 * @author orvigas@gmail.com
 */
@Component
@RequiredArgsConstructor
public class CustomMetrics {

  private final MeterRegistry meterRegistry;

  // Counters
  private Counter paymentCreatedCounter;
  private Counter paymentCompletedCounter;
  private Counter paymentFailedCounter;
  private Counter chargeSuccessCounter;
  private Counter chargeFailureCounter;

  // Timers
  private Timer paymentProcessingTimer;
  private Timer chargeProcessingTimer;

  // Gauges
  private AtomicInteger activePayments;
  private AtomicInteger circuitBreakerOpenCount;

  @PostConstruct
  private void initializeAfterConstruction() {
    initializeMetrics();
  }

  private void initializeMetrics() {
    // Counters
    paymentCreatedCounter = Counter.builder("payment.created")
        .description("Total payments created")
        .register(meterRegistry);

    paymentCompletedCounter = Counter.builder("payment.completed")
        .description("Total payments completed successfully")
        .register(meterRegistry);

    paymentFailedCounter = Counter.builder("payment.failed")
        .description("Total payments that failed")
        .register(meterRegistry);

    chargeSuccessCounter = Counter.builder("charge.success")
        .description("Total successful charges")
        .register(meterRegistry);

    chargeFailureCounter = Counter.builder("charge.failure")
        .description("Total failed charges")
        .register(meterRegistry);

    // Timers
    paymentProcessingTimer = Timer.builder("payment.processing.duration")
        .description("Time to process payment")
        .publishPercentiles(0.5, 0.95, 0.99) // P50, P95, P99
        .register(meterRegistry);

    chargeProcessingTimer = Timer.builder("charge.processing.duration")
        .description("Time to charge payment")
        .publishPercentiles(0.5, 0.95, 0.99)
        .register(meterRegistry);

    // Gauges
    activePayments = new AtomicInteger(0);
    meterRegistry.gauge("payment.active", activePayments);

    circuitBreakerOpenCount = new AtomicInteger(0);
    meterRegistry.gauge("circuitbreaker.open", circuitBreakerOpenCount);
  }

  // Counter methods
  public void incrementPaymentCreated() {
    paymentCreatedCounter.increment();
  }

  public void incrementPaymentCompleted() {
    paymentCompletedCounter.increment();
  }

  public void incrementPaymentFailed() {
    paymentFailedCounter.increment();
  }

  public void incrementChargeSuccess() {
    chargeSuccessCounter.increment();
  }

  public void incrementChargeFailure() {
    chargeFailureCounter.increment();
  }

  // Timer methods
  public Timer.Sample startPaymentProcessing() {
    return Timer.start(meterRegistry);
  }

  public void recordPaymentProcessing(Timer.Sample sample) {
    sample.stop(paymentProcessingTimer);
  }

  public Timer.Sample startChargeProcessing() {
    return Timer.start(meterRegistry);
  }

  public void recordChargeProcessing(Timer.Sample sample) {
    sample.stop(chargeProcessingTimer);
  }

  // Gauge methods
  public void setActivePayments(int count) {
    activePayments.set(count);
  }

  public void setCircuitBreakerOpenCount(int count) {
    circuitBreakerOpenCount.set(count);
  }
}
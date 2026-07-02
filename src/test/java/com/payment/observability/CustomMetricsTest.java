package com.payment.observability;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

/**
 * Tests for CustomMetrics.
 *
 * @author orvigas@gmail.com
 */
@SpringBootTest
class CustomMetricsTest {

  @Autowired
  private CustomMetrics customMetrics;

  @Autowired
  private MeterRegistry meterRegistry;

  @BeforeEach
  void setUp() {
  }

  @Test
  void testIncrementPaymentCreated() {
    customMetrics.incrementPaymentCreated();
    assertNotNull(meterRegistry.find("payment.created").counter());
  }

  @Test
  void testIncrementPaymentCompleted() {
    customMetrics.incrementPaymentCompleted();
    assertNotNull(meterRegistry.find("payment.completed").counter());
  }

  @Test
  void testIncrementPaymentFailed() {
    customMetrics.incrementPaymentFailed();
    assertNotNull(meterRegistry.find("payment.failed").counter());
  }

  @Test
  void testIncrementChargeSuccess() {
    customMetrics.incrementChargeSuccess();
    assertNotNull(meterRegistry.find("charge.success").counter());
  }

  @Test
  void testIncrementChargeFailure() {
    customMetrics.incrementChargeFailure();
    assertNotNull(meterRegistry.find("charge.failure").counter());
  }

  @Test
  void testStartPaymentProcessing() {
    Timer.Sample sample = customMetrics.startPaymentProcessing();
    assertNotNull(sample);
  }

  @Test
  void testRecordPaymentProcessing() {
    Timer.Sample sample = customMetrics.startPaymentProcessing();
    customMetrics.recordPaymentProcessing(sample);
    assertNotNull(meterRegistry.find("payment.processing.duration").timer());
  }

  @Test
  void testStartChargeProcessing() {
    Timer.Sample sample = customMetrics.startChargeProcessing();
    assertNotNull(sample);
  }

  @Test
  void testRecordChargeProcessing() {
    Timer.Sample sample = customMetrics.startChargeProcessing();
    customMetrics.recordChargeProcessing(sample);
    assertNotNull(meterRegistry.find("charge.processing.duration").timer());
  }

  @Test
  void testSetActivePayments() {
    customMetrics.setActivePayments(5);
    assertNotNull(meterRegistry.find("payment.active").gauge());
  }

  @Test
  void testSetCircuitBreakerOpenCount() {
    customMetrics.setCircuitBreakerOpenCount(2);
    assertNotNull(meterRegistry.find("circuitbreaker.open").gauge());
  }

  @Test
  void testMultipleMetricOperations() {
    customMetrics.incrementPaymentCreated();
    customMetrics.incrementPaymentCompleted();
    customMetrics.incrementChargeSuccess();
    customMetrics.setActivePayments(3);

    assertNotNull(meterRegistry.find("payment.created").counter());
    assertNotNull(meterRegistry.find("payment.completed").counter());
    assertNotNull(meterRegistry.find("charge.success").counter());
    assertNotNull(meterRegistry.find("payment.active").gauge());
  }

  @Test
  void testTimerSampling() {
    Timer.Sample sample1 = customMetrics.startPaymentProcessing();
    Timer.Sample sample2 = customMetrics.startChargeProcessing();

    customMetrics.recordPaymentProcessing(sample1);
    customMetrics.recordChargeProcessing(sample2);

    assertNotNull(meterRegistry.find("payment.processing.duration").timer());
    assertNotNull(meterRegistry.find("charge.processing.duration").timer());
  }
}

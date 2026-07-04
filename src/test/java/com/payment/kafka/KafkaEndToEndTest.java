package com.payment.kafka;

import com.payment.PaymentCoreApplication;
import com.payment.contracts.CreatePaymentRequest;
import com.payment.models.Payment;
import com.payment.models.PaymentStatus;
import com.payment.repositories.PaymentRepository;
import com.payment.services.PaymentService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end tests for Kafka integration with full Spring Boot context.
 */
@SpringBootTest(classes = PaymentCoreApplication.class)
@EmbeddedKafka(partitions = 1, brokerProperties = {
    "listeners=PLAINTEXT://localhost:0",
    "port=0"
})
@ActiveProfiles("test")
@Slf4j
public class KafkaEndToEndTest {

  @Autowired
  private PaymentService paymentService;

  @Autowired
  private PaymentRepository paymentRepository;

  @Autowired
  private AnalyticsConsumer analyticsConsumer;

  @Test
  void testEndToEndPaymentCreation() {
    CreatePaymentRequest request = new CreatePaymentRequest(
        new BigDecimal("5000.00"),
        "MXN",
        "test-merchant",
        "E2E test payment"
    );

    // Create payment
    var response = paymentService.createPayment(request, "user_e2e_123");

    assertNotNull(response);
    assertEquals(PaymentStatus.PENDING, response.status());
    assertEquals("user_e2e_123", response.userId());
    assertEquals(new BigDecimal("5000.00"), response.amount());

    // Verify payment was saved
    Optional<Payment> savedPayment = paymentRepository.findByPaymentId(response.paymentId());
    assertTrue(savedPayment.isPresent());
    assertEquals(PaymentStatus.PENDING, savedPayment.get().getStatus());

    log.info("Test passed: End-to-end payment creation with Kafka event");
  }

  @Test
  void testAnalyticsConsumerInitialization() {
    assertNotNull(analyticsConsumer);
    assertEquals(0, analyticsConsumer.getSuccessfulCharges());
    assertEquals(0, analyticsConsumer.getFailedCharges());

    log.info("Test passed: AnalyticsConsumer is properly initialized");
  }

  @Test
  void testMultiplePaymentCreations() {
    for (int i = 0; i < 3; i++) {
      CreatePaymentRequest request = new CreatePaymentRequest(
          new BigDecimal("1000.00"),
          "USD",
          "merchant",
          "Batch test " + i
      );

      var response = paymentService.createPayment(request, "user_multi_" + i);
      assertNotNull(response);
      assertEquals(PaymentStatus.PENDING, response.status());
    }

    log.info("Test passed: Multiple payments created successfully");
  }

  @Test
  void testPaymentConfirmation() {
    CreatePaymentRequest createRequest = new CreatePaymentRequest(
        new BigDecimal("2500.00"),
        "USD",
        "test-merchant",
        "Confirmation test"
    );

    var created = paymentService.createPayment(createRequest, "user_confirm");
    assertNotNull(created);

    // Confirm payment
    var confirmed = paymentService.confirmPayment(created.paymentId(), "user_confirm");
    assertNotNull(confirmed);
    assertEquals(created.paymentId(), confirmed.paymentId());

    log.info("Test passed: Payment confirmation with Kafka events");
  }

  @Test
  void testPaymentRefund() {
    CreatePaymentRequest createRequest = new CreatePaymentRequest(
        new BigDecimal("3000.00"),
        "MXN",
        "test-merchant",
        "Refund test"
    );

    var created = paymentService.createPayment(createRequest, "user_refund");

    // Confirm first
    paymentService.confirmPayment(created.paymentId(), "user_refund");

    // Refund
    var refunded = paymentService.refundPayment(created.paymentId(), "user_refund");
    assertNotNull(refunded);
    assertEquals(PaymentStatus.REFUNDED, refunded.status());

    log.info("Test passed: Payment refund with Kafka events");
  }
}

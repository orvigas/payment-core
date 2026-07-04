package com.payment.services;

import com.payment.contracts.CreatePaymentRequest;
import com.payment.models.Payment;
import com.payment.models.PaymentStatus;
import com.payment.errors.InvalidPaymentException;
import com.payment.errors.PaymentAccessDeniedException;
import com.payment.errors.PaymentNotFoundException;
import com.payment.observability.CustomMetrics;
import com.payment.repositories.PaymentRepository;
import com.payment.kafka.PaymentProducer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link PaymentService}.
 *
 * @author orvigas@gmail.com
 */
@ExtendWith(MockitoExtension.class)
public class PaymentServiceTest {

  @Mock
  private PaymentRepository paymentRepository;

  @Mock
  private PaymentValidator paymentValidator;

  @Mock
  private PaymentProducer paymentProducer;

  @Mock
  private CustomMetrics customMetrics;

  @InjectMocks
  private PaymentService paymentService;

  private CreatePaymentRequest validRequest;

  @BeforeEach
  void setUp() {
    validRequest = new CreatePaymentRequest(
        new BigDecimal("5000.00"),
        "MXN",
        "jersey-mikes",
        null);
  }

  @Test
  void testCreatePaymentSuccess() {
    // Arrange
    Payment savedPayment = new Payment();
    savedPayment.setPaymentId("pay_123");
    savedPayment.setStatus(PaymentStatus.PENDING);

    when(paymentRepository.save(any())).thenReturn(savedPayment);

    // Act
    var response = paymentService.createPayment(validRequest, "user123");

    // Assert
    assertNotNull(response);
    assertEquals("pay_123", response.paymentId());
    assertEquals(PaymentStatus.PENDING, response.status());
    verify(paymentRepository, times(1)).save(any());
  }

  @Test
  void testCreatePaymentUsesAuthenticatedOwner() {
    // Arrange
    Payment savedPayment = new Payment();
    savedPayment.setPaymentId("pay_123");
    savedPayment.setStatus(PaymentStatus.PENDING);

    when(paymentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    // Act
    paymentService.createPayment(validRequest, "authenticated-user");

    // Assert - the owner comes from the authenticated caller, never the request body
    verify(paymentRepository).save(argThat(payment -> "authenticated-user".equals(payment.getUserId())));
  }

  @Test
  void testGetPaymentNotFound() {
    // Arrange
    when(paymentRepository.findByPaymentId("invalid")).thenReturn(Optional.empty());

    // Act & Assert
    assertThrows(PaymentNotFoundException.class, () -> {
      paymentService.getPayment("invalid", "user123");
    });
  }

  @Test
  void testGetPaymentDeniedForDifferentUser() {
    // Arrange
    Payment payment = new Payment();
    payment.setPaymentId("pay_123");
    payment.setUserId("owner");
    payment.setStatus(PaymentStatus.COMPLETED);

    when(paymentRepository.findByPaymentId("pay_123")).thenReturn(Optional.of(payment));

    // Act & Assert
    assertThrows(PaymentAccessDeniedException.class, () -> {
      paymentService.getPayment("pay_123", "someone-else");
    });
  }

  @Test
  void testConfirmPaymentSuccess() {
    // Arrange
    Payment payment = new Payment();
    payment.setPaymentId("pay_123");
    payment.setUserId("user123");
    payment.setStatus(PaymentStatus.PENDING);

    when(paymentRepository.findByPaymentId("pay_123")).thenReturn(Optional.of(payment));
    when(paymentRepository.save(any())).thenReturn(payment);

    // Act
    var response = paymentService.confirmPayment("pay_123", "user123");

    // Assert
    assertNotNull(response);
    assertEquals("pay_123", response.paymentId());
    verify(paymentRepository, times(2)).save(any());
  }

  @Test
  void testConfirmPaymentNotFound() {
    // Arrange
    when(paymentRepository.findByPaymentId("invalid")).thenReturn(Optional.empty());

    // Act & Assert
    assertThrows(PaymentNotFoundException.class, () -> {
      paymentService.confirmPayment("invalid", "user123");
    });
  }

  @Test
  void testConfirmPaymentDeniedForDifferentUser() {
    // Arrange
    Payment payment = new Payment();
    payment.setPaymentId("pay_123");
    payment.setUserId("owner");
    payment.setStatus(PaymentStatus.PENDING);

    when(paymentRepository.findByPaymentId("pay_123")).thenReturn(Optional.of(payment));

    // Act & Assert
    assertThrows(PaymentAccessDeniedException.class, () -> {
      paymentService.confirmPayment("pay_123", "someone-else");
    });
    verify(paymentRepository, never()).save(any());
  }

  @Test
  void testConfirmPaymentAlreadyCompleted() {
    // Arrange
    Payment payment = new Payment();
    payment.setPaymentId("pay_123");
    payment.setUserId("user123");
    payment.setStatus(PaymentStatus.COMPLETED);

    when(paymentRepository.findByPaymentId("pay_123")).thenReturn(Optional.of(payment));

    // Act & Assert
    assertThrows(InvalidPaymentException.class, () -> {
      paymentService.confirmPayment("pay_123", "user123");
    });
  }

  @Test
  void testConfirmPaymentAlreadyRefunded() {
    // Arrange
    Payment payment = new Payment();
    payment.setPaymentId("pay_123");
    payment.setUserId("user123");
    payment.setStatus(PaymentStatus.REFUNDED);

    when(paymentRepository.findByPaymentId("pay_123")).thenReturn(Optional.of(payment));

    // Act & Assert
    assertThrows(InvalidPaymentException.class, () -> {
      paymentService.confirmPayment("pay_123", "user123");
    });
  }

  @Test
  void testRefundPaymentSuccess() {
    // Arrange
    Payment payment = new Payment();
    payment.setPaymentId("pay_123");
    payment.setUserId("user123");
    payment.setStatus(PaymentStatus.COMPLETED);

    when(paymentRepository.findByPaymentId("pay_123")).thenReturn(Optional.of(payment));
    when(paymentRepository.save(any())).thenReturn(payment);

    // Act
    var response = paymentService.refundPayment("pay_123", "user123");

    // Assert
    assertNotNull(response);
    assertEquals("pay_123", response.paymentId());
    verify(paymentRepository, times(1)).save(any());
  }

  @Test
  void testRefundPaymentNotFound() {
    // Arrange
    when(paymentRepository.findByPaymentId("invalid")).thenReturn(Optional.empty());

    // Act & Assert
    assertThrows(PaymentNotFoundException.class, () -> {
      paymentService.refundPayment("invalid", "user123");
    });
  }

  @Test
  void testRefundPaymentDeniedForDifferentUser() {
    // Arrange
    Payment payment = new Payment();
    payment.setPaymentId("pay_123");
    payment.setUserId("owner");
    payment.setStatus(PaymentStatus.COMPLETED);

    when(paymentRepository.findByPaymentId("pay_123")).thenReturn(Optional.of(payment));

    // Act & Assert
    assertThrows(PaymentAccessDeniedException.class, () -> {
      paymentService.refundPayment("pay_123", "someone-else");
    });
    verify(paymentRepository, never()).save(any());
  }

  @Test
  void testRefundPaymentNotCompleted() {
    // Arrange
    Payment payment = new Payment();
    payment.setPaymentId("pay_123");
    payment.setUserId("user123");
    payment.setStatus(PaymentStatus.PENDING);

    when(paymentRepository.findByPaymentId("pay_123")).thenReturn(Optional.of(payment));

    // Act & Assert
    assertThrows(InvalidPaymentException.class, () -> {
      paymentService.refundPayment("pay_123", "user123");
    });
  }

  @Test
  void testRefundPaymentProcessing() {
    // Arrange
    Payment payment = new Payment();
    payment.setPaymentId("pay_123");
    payment.setUserId("user123");
    payment.setStatus(PaymentStatus.PROCESSING);

    when(paymentRepository.findByPaymentId("pay_123")).thenReturn(Optional.of(payment));

    // Act & Assert
    assertThrows(InvalidPaymentException.class, () -> {
      paymentService.refundPayment("pay_123", "user123");
    });
  }

  @Test
  void testRefundPaymentAlreadyRefunded() {
    // Arrange
    Payment payment = new Payment();
    payment.setPaymentId("pay_123");
    payment.setUserId("user123");
    payment.setStatus(PaymentStatus.REFUNDED);

    when(paymentRepository.findByPaymentId("pay_123")).thenReturn(Optional.of(payment));

    // Act & Assert
    assertThrows(InvalidPaymentException.class, () -> {
      paymentService.refundPayment("pay_123", "user123");
    });
  }

  @Test
  void testCreatePaymentValidationFails() {
    // Arrange
    CreatePaymentRequest invalidRequest = new CreatePaymentRequest(
        new BigDecimal("-100.00"),
        "USD",
        "merchant",
        null);

    doThrow(new InvalidPaymentException("Amount must be positive"))
        .when(paymentValidator).validateCreatePaymentRequest(any());

    // Act & Assert
    assertThrows(InvalidPaymentException.class, () -> {
      paymentService.createPayment(invalidRequest, "user123");
    });
  }

  @Test
  void testGetPaymentFound() {
    // Arrange
    Payment payment = new Payment();
    payment.setPaymentId("pay_123");
    payment.setUserId("user123");
    payment.setStatus(PaymentStatus.COMPLETED);
    payment.setAmount(new BigDecimal("1000.00"));

    when(paymentRepository.findByPaymentId("pay_123")).thenReturn(Optional.of(payment));

    // Act
    var response = paymentService.getPayment("pay_123", "user123");

    // Assert
    assertNotNull(response);
    assertEquals("pay_123", response.paymentId());
    assertEquals(PaymentStatus.COMPLETED, response.status());
    verify(paymentRepository, times(1)).findByPaymentId("pay_123");
  }

  @Test
  void testCreatePaymentWithAllFields() {
    // Arrange
    CreatePaymentRequest request = new CreatePaymentRequest(
        new BigDecimal("2500.50"),
        "EUR",
        "new-merchant",
        "Purchase order #1234");

    Payment savedPayment = new Payment();
    savedPayment.setPaymentId("pay_456");
    savedPayment.setStatus(PaymentStatus.PENDING);
    savedPayment.setUserId("user456");
    savedPayment.setAmount(new BigDecimal("2500.50"));

    when(paymentRepository.save(any())).thenReturn(savedPayment);

    // Act
    var response = paymentService.createPayment(request, "user456");

    // Assert
    assertNotNull(response);
    assertEquals("pay_456", response.paymentId());
    assertEquals("user456", response.userId());
    assertEquals(new BigDecimal("2500.50"), response.amount());
  }

  @Test
  void testChargePaymentAsyncSuccess() throws ExecutionException, InterruptedException {
    // Arrange - stub the simulated processor so the outcome is deterministic
    Payment payment = new Payment();
    payment.setPaymentId("pay_123");
    payment.setStatus(PaymentStatus.PENDING);
    payment.setAmount(new BigDecimal("1000.00"));

    when(paymentRepository.findByPaymentId("pay_123")).thenReturn(Optional.of(payment));
    when(paymentRepository.save(any())).thenReturn(payment);

    PaymentService spyService = spy(paymentService);
    doReturn(true).when(spyService).callExternalProcessor(any());

    // Act
    CompletableFuture<com.payment.contracts.PaymentResponse> result = spyService.chargePaymentAsync("pay_123");

    // Assert - wait for completion
    assertNotNull(result.get());
    assertEquals(PaymentStatus.COMPLETED, payment.getStatus());
    verify(customMetrics, times(1)).startChargeProcessing();
    verify(customMetrics, times(1)).incrementChargeSuccess();
    verify(customMetrics, times(1)).recordChargeProcessing(any());
  }

  @Test
  void testChargePaymentAsyncChargeFailure() {
    // Arrange
    Payment payment = new Payment();
    payment.setPaymentId("pay_123");
    payment.setStatus(PaymentStatus.PENDING);

    when(paymentRepository.findByPaymentId("pay_123")).thenReturn(Optional.of(payment));

    PaymentService spyService = spy(paymentService);
    doReturn(false).when(spyService).callExternalProcessor(any());

    // Act
    CompletableFuture<com.payment.contracts.PaymentResponse> result = spyService.chargePaymentAsync("pay_123");

    // Assert
    ExecutionException ex = assertThrows(ExecutionException.class, () -> result.get());
    assertInstanceOf(RuntimeException.class, ex.getCause());
    verify(customMetrics, times(1)).incrementChargeFailure();
    verify(customMetrics, times(1)).recordChargeProcessing(any());
    verify(paymentRepository, never()).save(any());
  }

  @Test
  void testChargePaymentAsyncNotFound() {
    // Arrange
    when(paymentRepository.findByPaymentId("invalid")).thenReturn(Optional.empty());

    // Act & Assert
    CompletableFuture<com.payment.contracts.PaymentResponse> result = paymentService.chargePaymentAsync("invalid");
    assertThrows(ExecutionException.class, () -> result.get());
  }

  @Test
  void testChargerFallbackReturnsCurrentPaymentState() throws ExecutionException, InterruptedException {
    // Arrange
    Payment payment = new Payment();
    payment.setPaymentId("pay_123");
    payment.setStatus(PaymentStatus.PENDING);

    when(paymentRepository.findByPaymentId("pay_123")).thenReturn(Optional.of(payment));

    // Act
    CompletableFuture<com.payment.contracts.PaymentResponse> result = paymentService.chargerFallback("pay_123",
        new RuntimeException("circuit breaker open"));

    // Assert - payment is returned unchanged so the charge can be retried later
    assertEquals("pay_123", result.get().paymentId());
    assertEquals(PaymentStatus.PENDING, result.get().status());
    verify(customMetrics, times(1)).incrementChargeFailure();
  }

  @Test
  void testChargerFallbackPaymentNotFound() {
    // Arrange
    when(paymentRepository.findByPaymentId("invalid")).thenReturn(Optional.empty());

    // Act & Assert
    assertThrows(PaymentNotFoundException.class, () -> {
      paymentService.chargerFallback("invalid", new RuntimeException("circuit breaker open"));
    });
  }

}

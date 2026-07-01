package com.payment.service;

import com.payment.contracts.CreatePaymentRequest;
import com.payment.entity.Payment;
import com.payment.entity.PaymentStatus;
import com.payment.exception.InvalidPaymentException;
import com.payment.exception.PaymentNotFoundException;
import com.payment.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PaymentServiceTest {

  @Mock
  private PaymentRepository paymentRepository;

  @Mock
  private PaymentValidator paymentValidator;

  @InjectMocks
  private PaymentService paymentService;

  private CreatePaymentRequest validRequest;

  @BeforeEach
  void setUp() {
    validRequest = new CreatePaymentRequest(
        "user123",
        new BigDecimal("5000.00"),
        "MXN",
        "jersey-mikes",
        null
    );
  }

  @Test
  void testCreatePaymentSuccess() {
    // Arrange
    Payment savedPayment = new Payment();
    savedPayment.setPaymentId("pay_123");
    savedPayment.setStatus(PaymentStatus.PENDING);

    when(paymentRepository.save(any())).thenReturn(savedPayment);

    // Act
    var response = paymentService.createPayment(validRequest);

    // Assert
    assertNotNull(response);
    assertEquals("pay_123", response.paymentId());
    assertEquals(PaymentStatus.PENDING, response.status());
    verify(paymentRepository, times(1)).save(any());
  }

  @Test
  void testGetPaymentNotFound() {
    // Arrange
    when(paymentRepository.findByPaymentId("invalid")).thenReturn(Optional.empty());

    // Act & Assert
    assertThrows(PaymentNotFoundException.class, () -> {
      paymentService.getPayment("invalid");
    });
  }

  @Test
  void testConfirmPaymentSuccess() {
    // Arrange
    Payment payment = new Payment();
    payment.setPaymentId("pay_123");
    payment.setStatus(PaymentStatus.PENDING);

    when(paymentRepository.findByPaymentId("pay_123")).thenReturn(Optional.of(payment));
    when(paymentRepository.save(any())).thenReturn(payment);

    // Act
    var response = paymentService.confirmPayment("pay_123");

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
      paymentService.confirmPayment("invalid");
    });
  }

  @Test
  void testConfirmPaymentAlreadyCompleted() {
    // Arrange
    Payment payment = new Payment();
    payment.setPaymentId("pay_123");
    payment.setStatus(PaymentStatus.COMPLETED);

    when(paymentRepository.findByPaymentId("pay_123")).thenReturn(Optional.of(payment));

    // Act & Assert
    assertThrows(InvalidPaymentException.class, () -> {
      paymentService.confirmPayment("pay_123");
    });
  }

  @Test
  void testConfirmPaymentAlreadyRefunded() {
    // Arrange
    Payment payment = new Payment();
    payment.setPaymentId("pay_123");
    payment.setStatus(PaymentStatus.REFUNDED);

    when(paymentRepository.findByPaymentId("pay_123")).thenReturn(Optional.of(payment));

    // Act & Assert
    assertThrows(InvalidPaymentException.class, () -> {
      paymentService.confirmPayment("pay_123");
    });
  }

  @Test
  void testRefundPaymentSuccess() {
    // Arrange
    Payment payment = new Payment();
    payment.setPaymentId("pay_123");
    payment.setStatus(PaymentStatus.COMPLETED);

    when(paymentRepository.findByPaymentId("pay_123")).thenReturn(Optional.of(payment));
    when(paymentRepository.save(any())).thenReturn(payment);

    // Act
    var response = paymentService.refundPayment("pay_123");

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
      paymentService.refundPayment("invalid");
    });
  }

  @Test
  void testRefundPaymentNotCompleted() {
    // Arrange
    Payment payment = new Payment();
    payment.setPaymentId("pay_123");
    payment.setStatus(PaymentStatus.PENDING);

    when(paymentRepository.findByPaymentId("pay_123")).thenReturn(Optional.of(payment));

    // Act & Assert
    assertThrows(InvalidPaymentException.class, () -> {
      paymentService.refundPayment("pay_123");
    });
  }

  @Test
  void testRefundPaymentProcessing() {
    // Arrange
    Payment payment = new Payment();
    payment.setPaymentId("pay_123");
    payment.setStatus(PaymentStatus.PROCESSING);

    when(paymentRepository.findByPaymentId("pay_123")).thenReturn(Optional.of(payment));

    // Act & Assert
    assertThrows(InvalidPaymentException.class, () -> {
      paymentService.refundPayment("pay_123");
    });
  }

  @Test
  void testRefundPaymentAlreadyRefunded() {
    // Arrange
    Payment payment = new Payment();
    payment.setPaymentId("pay_123");
    payment.setStatus(PaymentStatus.REFUNDED);

    when(paymentRepository.findByPaymentId("pay_123")).thenReturn(Optional.of(payment));

    // Act & Assert
    assertThrows(InvalidPaymentException.class, () -> {
      paymentService.refundPayment("pay_123");
    });
  }

  @Test
  void testCreatePaymentValidationFails() {
    // Arrange
    CreatePaymentRequest invalidRequest = new CreatePaymentRequest(
        "user123",
        new BigDecimal("-100.00"),
        "USD",
        "merchant",
        null
    );

    doThrow(new InvalidPaymentException("Amount must be positive"))
        .when(paymentValidator).validateCreatePaymentRequest(any());

    // Act & Assert
    assertThrows(InvalidPaymentException.class, () -> {
      paymentService.createPayment(invalidRequest);
    });
  }

  @Test
  void testGetPaymentFound() {
    // Arrange
    Payment payment = new Payment();
    payment.setPaymentId("pay_123");
    payment.setStatus(PaymentStatus.COMPLETED);
    payment.setAmount(new BigDecimal("1000.00"));

    when(paymentRepository.findByPaymentId("pay_123")).thenReturn(Optional.of(payment));

    // Act
    var response = paymentService.getPayment("pay_123");

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
        "user456",
        new BigDecimal("2500.50"),
        "EUR",
        "new-merchant",
        "Purchase order #1234"
    );

    Payment savedPayment = new Payment();
    savedPayment.setPaymentId("pay_456");
    savedPayment.setStatus(PaymentStatus.PENDING);
    savedPayment.setUserId("user456");
    savedPayment.setAmount(new BigDecimal("2500.50"));

    when(paymentRepository.save(any())).thenReturn(savedPayment);

    // Act
    var response = paymentService.createPayment(request);

    // Assert
    assertNotNull(response);
    assertEquals("pay_456", response.paymentId());
    assertEquals("user456", response.userId());
    assertEquals(new BigDecimal("2500.50"), response.amount());
  }
}
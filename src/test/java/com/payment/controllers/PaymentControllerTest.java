package com.payment.controllers;

import com.payment.contracts.CreatePaymentRequest;
import com.payment.contracts.PaymentResponse;
import com.payment.models.PaymentStatus;
import com.payment.errors.InvalidPaymentException;
import com.payment.errors.PaymentNotFoundException;
import com.payment.services.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentControllerTest {

  @Mock
  private PaymentService paymentService;

  @InjectMocks
  private PaymentController paymentController;

  private CreatePaymentRequest createPaymentRequest;
  private PaymentResponse paymentResponse;

  @BeforeEach
  void setUp() {
    createPaymentRequest = new CreatePaymentRequest(
        "user123",
        new BigDecimal("1000.00"),
        "USD",
        "merchant-name",
        "Test payment"
    );

    paymentResponse = new PaymentResponse(
        "pay_123",
        "user123",
        new BigDecimal("1000.00"),
        "USD",
        "merchant-name",
        PaymentStatus.PENDING,
        "Test payment",
        LocalDateTime.now(),
        LocalDateTime.now(),
        null
    );
  }

  @Test
  void testCreatePaymentSuccess() {
    when(paymentService.createPayment(any(CreatePaymentRequest.class)))
        .thenReturn(paymentResponse);

    ResponseEntity<PaymentResponse> response = paymentController.createPayment(createPaymentRequest);

    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    assertEquals("pay_123", response.getBody().paymentId());
    assertEquals(PaymentStatus.PENDING, response.getBody().status());
    verify(paymentService).createPayment(any(CreatePaymentRequest.class));
  }

  @Test
  void testCreatePaymentWithValidation() {
    when(paymentService.createPayment(any(CreatePaymentRequest.class)))
        .thenThrow(new InvalidPaymentException("Invalid amount"));

    assertThrows(InvalidPaymentException.class, () -> {
      paymentController.createPayment(createPaymentRequest);
    });
  }

  @Test
  void testGetPaymentSuccess() {
    when(paymentService.getPayment(anyString()))
        .thenReturn(paymentResponse);

    ResponseEntity<PaymentResponse> response = paymentController.getPayment("pay_123");

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals("pay_123", response.getBody().paymentId());
    verify(paymentService).getPayment("pay_123");
  }

  @Test
  void testGetPaymentNotFound() {
    when(paymentService.getPayment(anyString()))
        .thenThrow(new PaymentNotFoundException("Payment not found: pay_999"));

    assertThrows(PaymentNotFoundException.class, () -> {
      paymentController.getPayment("pay_999");
    });
  }

  @Test
  void testConfirmPaymentSuccess() {
    PaymentResponse confirmedResponse = new PaymentResponse(
        "pay_123",
        "user123",
        new BigDecimal("1000.00"),
        "USD",
        "merchant-name",
        PaymentStatus.COMPLETED,
        "Test payment",
        LocalDateTime.now(),
        LocalDateTime.now(),
        LocalDateTime.now()
    );

    when(paymentService.confirmPayment(anyString()))
        .thenReturn(confirmedResponse);

    ResponseEntity<PaymentResponse> response = paymentController.confirmPayment("pay_123");

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(PaymentStatus.COMPLETED, response.getBody().status());
    verify(paymentService).confirmPayment("pay_123");
  }

  @Test
  void testConfirmPaymentInvalidStatus() {
    when(paymentService.confirmPayment(anyString()))
        .thenThrow(new InvalidPaymentException("Only PENDING payments can be confirmed"));

    assertThrows(InvalidPaymentException.class, () -> {
      paymentController.confirmPayment("pay_123");
    });
  }

  @Test
  void testConfirmPaymentNotFound() {
    when(paymentService.confirmPayment(anyString()))
        .thenThrow(new PaymentNotFoundException("Payment not found: pay_999"));

    assertThrows(PaymentNotFoundException.class, () -> {
      paymentController.confirmPayment("pay_999");
    });
  }

  @Test
  void testRefundPaymentSuccess() {
    PaymentResponse refundedResponse = new PaymentResponse(
        "pay_123",
        "user123",
        new BigDecimal("1000.00"),
        "USD",
        "merchant-name",
        PaymentStatus.REFUNDED,
        "Test payment",
        LocalDateTime.now(),
        LocalDateTime.now(),
        LocalDateTime.now()
    );

    when(paymentService.refundPayment(anyString()))
        .thenReturn(refundedResponse);

    ResponseEntity<PaymentResponse> response = paymentController.refundPayment("pay_123");

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(PaymentStatus.REFUNDED, response.getBody().status());
    verify(paymentService).refundPayment("pay_123");
  }

  @Test
  void testRefundPaymentInvalidStatus() {
    when(paymentService.refundPayment(anyString()))
        .thenThrow(new InvalidPaymentException("Only COMPLETED payments can be refunded"));

    assertThrows(InvalidPaymentException.class, () -> {
      paymentController.refundPayment("pay_123");
    });
  }

  @Test
  void testRefundPaymentNotFound() {
    when(paymentService.refundPayment(anyString()))
        .thenThrow(new PaymentNotFoundException("Payment not found: pay_999"));

    assertThrows(PaymentNotFoundException.class, () -> {
      paymentController.refundPayment("pay_999");
    });
  }
}
